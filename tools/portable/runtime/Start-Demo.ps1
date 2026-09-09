param([switch]$NoBrowser, [ValidateRange(10,600)][int]$StartupTimeoutSeconds = 120)
. (Join-Path $PSScriptRoot 'Portable.Common.ps1')
$root = Get-PackageRoot $PSScriptRoot
$runLock = $null; $startupGuard = $null; $dbChild = $null; $appChild = $null; $initChild = $null
$ownsRun = $false; $failure = $false
try {
    Assert-PortablePrerequisites $root
    Assert-NoReparsePath $root
    Assert-PortsFree
    $phase = Get-DataPhase $root
    $null = New-Item -ItemType Directory -Path (Join-Path $root 'var') -Force
    try { $runLock = [IO.File]::Open((Join-Path $root 'var/run.lock'),'OpenOrCreate','ReadWrite','None') }
    catch { throw 'A package supervisor is already active. Use Stop-Demo first.' }
    $startupGuard = Enter-LifecycleLock $root
    if (Test-Path -LiteralPath (Join-Path $root 'var/run-state.json')) { throw 'Previous run state exists. Run Stop-Demo to verify and close it before starting again.' }
    Protect-PrivateDirectory (Join-Path $root 'var/private')
    $null = New-Item -ItemType Directory -Path (Join-Path $root 'var/logs') -Force
    $private = Join-Path $root 'var/private'
    $serverFile = Join-Path $private 'my.ini'
    Write-MySqlConfig $serverFile (Get-ServerConfig $root)
    $bootstrap = Join-Path $private 'bootstrap.sql'
    if ($phase -eq 'Fresh') {
        Write-Utf8 (Join-Path $root 'var/initialization.started') 'started'
        $rootSecret = New-LocalSecret; $appSecret = New-LocalSecret
        Write-MySqlConfig (Join-Path $private 'root.cnf') (Get-ClientConfig $rootSecret)
        Write-Utf8 (Join-Path $private 'app.secret') $appSecret
        Write-Utf8 $bootstrap (Get-BootstrapSql $rootSecret $appSecret)
        $rootSecret = $null
        $initInfo = New-ChildStartInfo (Join-Path $root 'runtime/mysql/bin/mysqld.exe') @("--defaults-file=$serverFile",'--initialize-insecure') $root
        $initExitMarker = Join-Path $root 'var/initialization-exit-unconfirmed'
        Write-Utf8 $initExitMarker 'unconfirmed'
        $initChild = Start-LoggedChild $initInfo (Join-Path $root 'var/logs/mysql-initialize')
        $initChild.Process.StandardInput.Close()
        if (!$initChild.Process.WaitForExit(120000)) { throw 'MySQL initialization timed out. Preserve var and wait/inspect the initialization process; never delete and retry.' }
        Remove-Item -LiteralPath $initExitMarker -Force
        if ($initChild.Process.ExitCode -ne 0) { throw 'MySQL initialization failed. Preserve var; see var/logs/mysql-error.log. Automatic retry is disabled.' }
        Close-ChildLogs $initChild; $initChild = $null
    } else {
        $appSecret = [IO.File]::ReadAllText((Join-Path $private 'app.secret')).Trim()
        if ($appSecret -notmatch '^[a-f0-9]{64}$') { throw 'Invalid local application secret; preserve var.' }
    }
    $runId = [guid]::NewGuid().ToString('N')
    $stopFile = Join-Path $private ($runId + '.stop')
    $state = [pscustomobject]@{ Version=1; Root=$root; RunId=$runId; StopFile=$stopFile; Database=$null; App=$null }
    Save-RunState $root $state
    $ownsRun = $true
    # Keep bootstrap diagnostics private: failing SQL can contain credentials.
    $dbArgs = @(Get-MySqlServerArguments $root -Bootstrap:($phase -eq 'Fresh'))
    Assert-PortsFree
    $dbInfo = New-ChildStartInfo (Join-Path $root 'runtime/mysql/bin/mysqld.exe') $dbArgs $root
    $dbLog = if ($phase -eq 'Fresh') { Join-Path $private 'mysql-bootstrap' } else { Join-Path $root 'var/logs/mysql' }
    $dbChild = Start-LoggedChild $dbInfo $dbLog
    $dbChild.Process.StandardInput.Close()
    $state.Database = Get-ProcessIdentity $dbChild.Process.Id
    if (!$state.Database) { throw 'MySQL exited before its identity could be recorded.' }
    Save-RunState $root $state
    $deadline = [DateTime]::UtcNow.AddSeconds($StartupTimeoutSeconds)
    while ($true) {
        if ($dbChild.Process.HasExited) { throw 'Private MySQL exited; inspect package logs (bootstrap diagnostics are in var/private).' }
        try { Assert-OwnedListener 13306 $state.Database.Id; Invoke-PrivateClient $root @('--batch','--skip-column-names') 'SELECT 1;'; break }
        catch {
            if ([DateTime]::UtcNow -gt $deadline) { throw 'Private MySQL did not become ready. Preserve var and inspect package logs.' }
            Start-Sleep -Milliseconds 500
        }
    }
    if ($phase -eq 'Fresh') {
        Invoke-PrivateClient $root @('--batch') ([IO.File]::ReadAllText((Join-Path $root 'database/schema.sql'),[Text.Encoding]::UTF8))
        Invoke-PrivateClient $root @('--batch') ([IO.File]::ReadAllText((Join-Path $root 'database/data.sql'),[Text.Encoding]::UTF8))
        Write-Utf8 (Join-Path $root 'var/initialized') 'complete'
        # Remove only the transient generated credential SQL, never data or diagnostics.
        Remove-Item -LiteralPath $bootstrap -Force
    }
    $appFile = Join-Path $private 'application.properties'
    Write-Utf8 $appFile (Get-AppConfig $appSecret $stopFile)
    $appSecret = $null
    $appUri = ([uri]$appFile).AbsoluteUri
    $appEnvironment = @{
        SPRING_CONFIG_ADDITIONAL_LOCATION=$appUri
        DB_HOST='127.0.0.1'; DB_PORT='13306'; DB_NAME='shandong_weather'; DB_USERNAME='weather_demo'
        SERVER_ADDRESS='127.0.0.1'; SERVER_PORT='18080'
    }
    $javaInfo = New-ChildStartInfo (Join-Path $root 'runtime/java/bin/java.exe') (Get-JavaArguments $root $stopFile) $root $appEnvironment
    $appChild = Start-LoggedChild $javaInfo (Join-Path $root 'var/logs/application')
    $appChild.Process.StandardInput.Close()
    $state.App = Get-ProcessIdentity $appChild.Process.Id
    if (!$state.App) { throw 'Application exited before its identity could be recorded.' }
    Save-RunState $root $state
    $deadline = [DateTime]::UtcNow.AddSeconds($StartupTimeoutSeconds)
    while ($true) {
        if ($appChild.Process.HasExited -or $dbChild.Process.HasExited) { throw 'Application or private MySQL exited during startup; inspect var/logs.' }
        $ready = $false
        try {
            if (!(Assert-OwnedProcess $root $state.App App)) { throw 'Application exited.' }
            Assert-OwnedListener 18080 $state.App.Id
            $request = [Net.HttpWebRequest]::Create('http://127.0.0.1:18080/api/cities')
            $request.Proxy = $null; $request.Timeout = 2000; $request.ReadWriteTimeout = 2000; $request.AllowAutoRedirect = $false
            $response = $request.GetResponse()
            try {
                $reader = New-Object IO.StreamReader($response.GetResponseStream(),[Text.Encoding]::UTF8)
                try { $ready = ([int]$response.StatusCode -eq 200) -and (Test-DictionaryResponse $reader.ReadToEnd()) }
                finally { $reader.Dispose() }
            } finally { $response.Dispose() }
        } catch { $ready = $false }
        if ($ready) { Assert-OwnedListener 18080 $state.App.Id; break }
        if ([DateTime]::UtcNow -gt $deadline) { throw 'Real dictionary GET did not succeed before the startup deadline. Browser was not opened.' }
        Start-Sleep -Milliseconds 500
    }
    Write-Host 'Demo ready: http://127.0.0.1:18080/management'
    Write-Host 'Keep this supervisor open. Use Stop-Demo.ps1 or the package stop command to exit gracefully.'
    if (!$NoBrowser) { Start-Process 'http://127.0.0.1:18080/management' }
    $startupGuard.Dispose(); $startupGuard = $null
    while (!$appChild.Process.HasExited -and !$dbChild.Process.HasExited) { Start-Sleep -Milliseconds 500 }
} catch {
    $failure = $true
    Write-Host ('Startup stopped: ' + $_.Exception.Message) -ForegroundColor Red
} finally {
    # Stop-PortablePackage acquires the same lock; release even on startup failure.
    if ($startupGuard) { $startupGuard.Dispose(); $startupGuard = $null }
    if ($ownsRun) {
        try { Stop-PortablePackage $root }
        catch { $failure = $true; Write-Host ('Graceful cleanup incomplete: ' + $_.Exception.Message) -ForegroundColor Red }
    }
    foreach ($child in @($appChild,$dbChild,$initChild)) { if ($child) { Close-ChildLogs $child } }
    if ($runLock) { $runLock.Dispose() }
}
if ($failure) { exit 1 }
