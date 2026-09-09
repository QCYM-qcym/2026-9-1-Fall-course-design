$ErrorActionPreference = 'Stop'
$module = Join-Path $PSScriptRoot '../runtime/Portable.Common.ps1'
if (!(Test-Path -LiteralPath $module)) { throw 'FAIL: portable runtime safety functions are missing' }
. $module
$script:count = 0
function Check($ok, $name) { if (!$ok) { throw "FAIL: $name" }; $script:count++ }
function Throws($action, $name) { $failed = $false; try { & $action } catch { $failed = $true }; Check $failed $name }
$root = Join-Path ([IO.Path]::GetTempPath()) ('portable fixtures ' + [char]0x4e2d + ' ' + [guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path $root
try {
    Check ((Get-PackageRoot (Join-Path $root 'runtime/scripts')) -eq $root) 'root is two levels above scripts'
    Check ((Get-DataPhase $root) -eq 'Fresh') 'absent data is fresh'
    $null = New-Item -ItemType Directory -Path (Join-Path $root 'var/mysql') -Force
    Throws { Get-DataPhase $root } 'existing unmarked data must never initialize'
    $null = New-Item -ItemType Directory -Path (Join-Path $root 'var/private') -Force
    [IO.File]::WriteAllText((Join-Path $root 'var/initialized'), 'complete')
    Throws { Get-DataPhase $root } 'marker alone cannot authorize existing data'
    $cfg = Get-ServerConfig $root
    foreach ($line in @('port=13306','bind-address=127.0.0.1','mysqlx=0','skip-name-resolve=ON')) {
        Check ($cfg.Split("`n").Trim() -contains $line) "server isolation: $line"
    }
    Check ($cfg -notmatch '(?m)^\s*admin[-_]address\s*=') 'admin interface remains unconfigured instead of receiving an invalid empty address'
    $props = Get-AppConfig '0123456789abcdef'
    Check ($props -match 'jdbc:mysql://127.0.0.1:13306/shandong_weather\?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true') 'JDBC parameters retained'
    Check ($props -match 'server.address=127.0.0.1') 'app loopback only'
    Check ($props -match 'server.port=18080') 'app dedicated port'
    $expected = [pscustomobject]@{ Id=123; Exe='C:\demo\java.exe'; CommandLine='"C:\demo\java.exe" PortableLauncher'; StartTicks='123456' }
    $actual = [pscustomobject]@{ Id=123; Exe='C:\demo\java.exe'; CommandLine='"C:\demo\java.exe" PortableLauncher'; StartTicks='123456' }
    Check (Test-ProcessIdentity $expected $actual) 'matching identity accepted'
    foreach ($field in @('Id','Exe','CommandLine','StartTicks')) {
        $copy = $actual.PSObject.Copy(); $copy.$field = '999'
        Check (!(Test-ProcessIdentity $expected $copy)) "foreign $field rejected"
    }
    Check (!(Test-ProcessIdentity $expected $null)) 'missing process rejected'
    Check (Test-DictionaryResponse '{"code":200,"data":[{"id":1,"cityCode":"JINAN"}]}') 'real dictionary response accepted'
    foreach ($body in @('{}','{"code":500,"data":[]}','{"code":200,"data":[]}','<html>ok</html>')) {
        Check (!(Test-DictionaryResponse $body)) 'false readiness rejected'
    }
    Check ((ConvertTo-NativeArgument 'C:\demo path\') -eq '"C:\demo path\\"') 'trailing slash quoted safely'
    Check ((New-LocalSecret) -match '^[a-f0-9]{64}$') 'secret is crypto-safe and config-safe'
    $startInfo = New-ChildStartInfo 'C:\demo\java.exe' @('-jar','C:\demo path\weather-demo.jar') $root @{ DB_HOST='127.0.0.1'; DB_PORT='13306' }
    Check ($startInfo.Arguments -eq '"-jar" "C:\demo path\weather-demo.jar"') 'process arguments retain spaced paths'
    Check ($startInfo.EnvironmentVariables['DB_PORT'] -eq '13306') 'private child DB port'
    $previousPassword = $env:DB_PASSWORD; $previousSpringJson = $env:SPRING_APPLICATION_JSON
    $env:DB_PASSWORD = 'fixture-do-not-inherit'
    $env:SPRING_APPLICATION_JSON = '{"server":{"port":3306}}'
    try {
        $info = New-ChildStartInfo 'C:\demo\java.exe' @() $root
        Check (!$info.EnvironmentVariables.ContainsKey('DB_PASSWORD')) 'development password is not inherited'
        Check (!$info.EnvironmentVariables.ContainsKey('SPRING_APPLICATION_JSON')) 'higher-priority Spring overrides are removed'
    } finally { $env:DB_PASSWORD = $previousPassword; $env:SPRING_APPLICATION_JSON = $previousSpringJson }
    $unicodeStop = 'C:/demo ' + [char]0x4e2d + '/a.stop'
    Check ((Get-AppConfig 'abc' $unicodeStop) -match 'weather.portable.stop-file=C:/demo \\u4e2d/a.stop') 'properties Unicode path preserved with Java escapes'
    $fresh = Join-Path $root 'interrupted'
    $null = New-Item -ItemType Directory -Path (Join-Path $fresh 'var') -Force
    [IO.File]::WriteAllText((Join-Path $fresh 'var/initialization.started'), 'started')
    Throws { Get-DataPhase $fresh } 'interrupted init without datadir is not retried'
    $sql = Get-BootstrapSql ('a' * 64) ('b' * 64)
    Check ($sql -match "GRANT SELECT, INSERT, UPDATE, DELETE ON shandong_weather\.\* TO 'weather_demo'@'127.0.0.1'") 'application grants are limited to four DML privileges'
    Throws { Get-BootstrapSql "bad'password" ('b' * 64) } 'unsafe bootstrap secret rejected'
    $entry = Join-Path $PSScriptRoot '../runtime/Start-Demo.ps1'
    Check (Test-Path -LiteralPath $entry) 'start entry is available for package prerequisite test'
    $scripts = Join-Path $root 'runtime/scripts'
    $null = New-Item -ItemType Directory -Path $scripts -Force
    Copy-Item -Path (Join-Path $PSScriptRoot '../runtime/*.ps1') -Destination $scripts
    $incomplete = Join-Path $root 'incomplete'
    $null = New-Item -ItemType Directory -Path (Join-Path $incomplete 'runtime/scripts') -Force
    Copy-Item -Path (Join-Path $scripts '*.ps1') -Destination (Join-Path $incomplete 'runtime/scripts')
    $ps = Join-Path $PSHOME 'powershell.exe'
    $info = New-ChildStartInfo $ps @('-NoProfile','-ExecutionPolicy','Bypass','-File',(Join-Path $incomplete 'runtime/scripts/Start-Demo.ps1'),'-NoBrowser') $incomplete
    $p = [Diagnostics.Process]::Start($info)
    $output = $p.StandardOutput.ReadToEndAsync(); $errors = $p.StandardError.ReadToEndAsync()
    Check ($p.WaitForExit(15000)) 'incomplete package fails promptly'
    Check ($p.ExitCode -ne 0) 'incomplete package rejects startup'
    Check (!(Test-Path -LiteralPath (Join-Path $incomplete 'var'))) 'prerequisite failure makes no runtime state'
    $p.Dispose()
    Check (Test-ListenerIdentity @([pscustomobject]@{Port=18080;Address='127.0.0.1';Id=123}) 18080 123) 'owned loopback listener accepted'
    Check (!(Test-ListenerIdentity @([pscustomobject]@{Port=18080;Address='127.0.0.1';Id=999}) 18080 123)) 'foreign listener cannot satisfy readiness'
    Check (!(Test-ListenerIdentity @([pscustomobject]@{Port=18080;Address='0.0.0.0';Id=123}) 18080 123)) 'wildcard listener rejected'
    Check (!(Test-ListenerIdentity @() 18080 123)) 'absent listener cannot satisfy readiness'
    $idle = Join-Path $root 'idle'
    $null = New-Item -ItemType Directory -Path $idle
    Stop-PortablePackage $idle
    Check (!(Test-Path -LiteralPath (Join-Path $idle 'var'))) 'stopping unused package does not create state'
    $stateRoot = Join-Path $root 'state fixture'
    $null = New-Item -ItemType Directory -Path (Join-Path $stateRoot 'var/private') -Force
    $runId = '0123456789abcdef0123456789abcdef'
    $state = [pscustomobject]@{Root=$stateRoot;RunId=$runId;StopFile=(Join-Path $stateRoot "var/private/$runId.stop");App=$expected;Database=$null}
    Save-RunState $stateRoot $state
    $saved = ConvertFrom-Json ([IO.File]::ReadAllText((Join-Path $stateRoot 'var/run-state.json')))
    Check ($saved.Root -eq $stateRoot) 'state round-trips Unicode package path'
    Throws { Stop-PortablePackage $stateRoot } 'foreign executable rejected before any process operation'
    Check (!(Test-Path -LiteralPath $state.StopFile)) 'foreign executable never receives stop-file signal'
    Check (Test-Path -LiteralPath (Join-Path $stateRoot 'var/run-state.json')) 'unsafe stop preserves state'
    $state.App = $null
    Save-RunState $stateRoot $state
    Stop-PortablePackage $stateRoot
    Check (!(Test-Path -LiteralPath (Join-Path $stateRoot 'var/run-state.json'))) 'completed empty run state is removed'
    Initialize-PortableNativeTcp
    Check ($null -ne ('PortableNativeTcp' -as [type])) 'native owner inspector compiles on PS5 without querying sockets'
    $javaArgs = Get-JavaArguments $root (Join-Path $root 'var/private/0123.stop')
    Check ($javaArgs -contains '--server.address=127.0.0.1') 'CLI fixes bind address above inherited Spring configuration'
    Check ($javaArgs -contains '--server.port=18080') 'CLI fixes application port'
    Check ($javaArgs -contains '--spring.datasource.url=jdbc:mysql://127.0.0.1:13306/shandong_weather?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true') 'CLI fixes private DB target'
    Check (!(($javaArgs -join ' ') -match 'password=')) 'Java command has no password property'
    $mysqlConfigPath = Join-Path $root 'mysql-ansi.ini'
    $chineseConfig = Get-ServerConfig $root
    Write-MySqlConfig $mysqlConfigPath $chineseConfig ([Text.Encoding]::GetEncoding(936))
    $configBytes = [IO.File]::ReadAllBytes($mysqlConfigPath)
    Check ([Text.Encoding]::GetEncoding(936).GetString($configBytes) -ceq $chineseConfig) 'MySQL config Chinese paths round-trip through Windows ANSI bytes'
    Check (!([Text.Encoding]::UTF8.GetString($configBytes) -ceq $chineseConfig)) 'Chinese MySQL configuration does not silently use UTF8 on ANSI Windows'
    $rejectedConfig = Join-Path $root 'unrepresentable.ini'
    Throws { Write-MySqlConfig $rejectedConfig $chineseConfig ([Text.Encoding]::ASCII) } 'unrepresentable MySQL path is rejected before initialization'
    Check (!(Test-Path -LiteralPath $rejectedConfig)) 'encoding rejection writes no corrupted config'
    $logPrefix = Join-Path $root 'cleanup-output'
    $childInfo = New-ChildStartInfo $ps @('-NoProfile','-Command',"Write-Output 'fixture log'") $root
    $loggedChild = Start-LoggedChild $childInfo $logPrefix
    $loggedChild.Process.StandardInput.Close()
    Check ($loggedChild.Process.WaitForExit(15000)) 'log fixture child exits'
    $cleanupOutput = @(Close-ChildLogs $loggedChild)
    Check ($cleanupOutput.Count -eq 0) 'log cleanup suppresses internal VoidTaskResult objects'
    Check (([IO.File]::ReadAllText($logPrefix + '.out.log')).Contains('fixture log')) 'log cleanup still drains stdout'
    $normalDbArgs = @(Get-MySqlServerArguments $root)
    Check ($normalDbArgs[0] -eq ('--defaults-file=' + (Join-Path $root 'var/private/my.ini'))) 'MySQL explicit defaults remain the first argument'
    Check ($normalDbArgs -contains '--no-monitor') 'ordinary MySQL runs directly without Unicode-breaking restart monitor'
    Check ($normalDbArgs.Count -eq 2) 'restart does not repeat bootstrap SQL'
    $bootstrapDbArgs = @(Get-MySqlServerArguments $root -Bootstrap)
    Check ($bootstrapDbArgs -contains '--no-monitor') 'first server run also bypasses restart monitor'
    Check ($bootstrapDbArgs -contains ('--init-file=' + (Join-Path $root 'var/private/bootstrap.sql'))) 'bootstrap path remains a complete Unicode argument'
    $dbInfo = New-ChildStartInfo (Join-Path $root 'runtime/mysql/bin/mysqld.exe') $bootstrapDbArgs $root
    Check ($dbInfo.Arguments.StartsWith(('"--defaults-file=' + (Join-Path $root 'var/private/my.ini') + '" "--no-monitor" '))) 'native launch preserves defaults ordering and direct-server mode'
    foreach ($adminMode in @($false,$true)) {
        $clientInfo = New-PrivateClientStartInfo $root @('shutdown') -Admin:$adminMode
        Check ($clientInfo.Arguments -eq '"--defaults-file=var/private/root.cnf" "shutdown"') 'private client uses ASCII defaults without unsupported login-path option'
        Check ($clientInfo.EnvironmentVariables['MYSQL_TEST_LOGIN_FILE'] -eq 'var/private/disabled-login.cnf') 'private client overrides external login-file lookup'
        Check ($clientInfo.WorkingDirectory -ceq $root) 'relative client defaults resolve against Unicode package root'
        $clientName = if ($adminMode) { 'mysqladmin.exe' } else { 'mysql.exe' }
        Check ($clientInfo.FileName -eq (Join-Path $root "runtime/mysql/bin/$clientName")) 'client executable remains scoped to package'
    }
    $disabledLogin = Join-Path $root 'var/private/disabled-login.cnf'
    [IO.File]::WriteAllText($disabledLogin, 'fixture')
    foreach ($adminMode in @($false,$true)) {
        Throws { New-PrivateClientStartInfo $root @('shutdown') -Admin:$adminMode } 'existing disabled-login file is refused instead of loaded'
    }
    Remove-Item -LiteralPath $disabledLogin
    $aclFolder = Join-Path $root 'acl-restart/private'
    Protect-PrivateDirectory $aclFolder
    $secretFixture = Join-Path $aclFolder 'retained.txt'
    [IO.File]::WriteAllText($secretFixture, 'retained fixture')
    Protect-PrivateDirectory $aclFolder
    Check ([IO.File]::ReadAllText($secretFixture) -eq 'retained fixture') 'repeated private ACL protection preserves existing runtime files'
    $readAcl = [IO.Directory]::GetAccessControl($aclFolder,[Security.AccessControl.AccessControlSections]::Access)
    Check $readAcl.AreAccessRulesProtected 'private DACL remains protected on restart'
    $preciseIdentity = $actual.PSObject.Copy(); $preciseIdentity.StartTicks = '639245899234567890'
    $identityJson = $preciseIdentity | ConvertTo-Json | ConvertFrom-Json
    Check (Test-ProcessIdentity $identityJson $preciseIdentity) 'start ticks remain exact strings across JSON serialization'
    $partialIdentity = $actual.PSObject.Copy()
    $partialIdentity.Exe = ''; $partialIdentity.CommandLine = ''
    Check ((Get-ExitObservation $expected $partialIdentity) -eq 'Exiting') 'read-only exit polling tolerates transient missing CIM fields'
    Check ((Get-ExitObservation $expected $null) -eq 'Exited') 'absent PID finishes exit polling'
    Check ((Get-ExitObservation $expected $actual) -eq 'Running') 'complete matching identity continues waiting'
    $reusedIdentity = $partialIdentity.PSObject.Copy(); $reusedIdentity.StartTicks = '999999'
    Throws { Get-ExitObservation $expected $reusedIdentity } 'PID reuse rejected even if other fields are temporarily missing'
    $raceRoot = Join-Path $root 'startup race'
    $null = New-Item -ItemType Directory -Path (Join-Path $raceRoot 'var/private') -Force
    $stopCommand = '. ''{0}''; try {{ Stop-PortablePackage ''{1}'' -LockTimeoutSeconds 1; exit 0 }} catch {{ [Console]::WriteLine($_.Exception.Message); exit 7 }}' -f ([IO.Path]::GetFullPath($module)).Replace("'","''"), $raceRoot.Replace("'","''")
    $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($stopCommand))
    $startupGuard = [IO.File]::Open((Join-Path $raceRoot 'var/stop.lock'),'OpenOrCreate','ReadWrite','None')
    try {
        $raceInfo = New-ChildStartInfo $ps @('-NoProfile','-EncodedCommand',$encoded) $raceRoot
        $raceProcess = [Diagnostics.Process]::Start($raceInfo)
        $raceOutput = $raceProcess.StandardOutput.ReadToEndAsync()
        $raceErrors = $raceProcess.StandardError.ReadToEndAsync()
        Check ($raceProcess.WaitForExit(5000)) 'stop attempt is bounded while startup owns lifecycle lock'
        Check ($raceProcess.ExitCode -eq 7) 'stop cannot claim success while startup has no state yet'
        Check ($raceOutput.GetAwaiter().GetResult() -match 'Startup or stop operation is still active') 'startup lock timeout reports an explicit busy failure'
        $raceProcess.Dispose()
    } finally { $startupGuard.Dispose() }
    $startupGuard = Enter-LifecycleLock $raceRoot 1
    try {
        $pendingState = [pscustomobject]@{Root=$raceRoot;RunId=$runId;StopFile=(Join-Path $raceRoot "var/private/$runId.stop");App=$null;Database=$null}
        Save-RunState $raceRoot $pendingState
        $raceProcess = [Diagnostics.Process]::Start($raceInfo)
        $raceOutput = $raceProcess.StandardOutput.ReadToEndAsync()
        $raceErrors = $raceProcess.StandardError.ReadToEndAsync()
        Check ($raceProcess.WaitForExit(5000)) 'stop waits only to deadline during empty-state registration window'
        Check ($raceProcess.ExitCode -eq 7) 'empty state cannot bypass startup lifecycle guard'
        Check (Test-Path -LiteralPath (Join-Path $raceRoot 'var/run-state.json')) 'concurrent stop cannot remove partially registered run state'
        $raceProcess.Dispose()
    } finally { $startupGuard.Dispose() }
    Stop-PortablePackage $raceRoot -LockTimeoutSeconds 1
    Check (!(Test-Path -LiteralPath (Join-Path $raceRoot 'var/run-state.json'))) 'stop processes state only after startup guard releases'
    $unconfirmedMarker = Join-Path $raceRoot 'var/initialization-exit-unconfirmed'
    [IO.File]::WriteAllText($unconfirmedMarker, 'unconfirmed')
    Throws { Get-DataPhase $raceRoot } 'lone unconfirmed initialization marker must not be classified Fresh'
    Throws { Stop-PortablePackage $raceRoot -LockTimeoutSeconds 1 } 'unconfirmed initialization exit cannot report stopped after lifecycle lock release'
    Check (Test-Path -LiteralPath $unconfirmedMarker) 'stop preserves unconfirmed initialization marker'
    Check (!(Test-Path -LiteralPath (Join-Path $raceRoot 'var/run-state.json'))) 'unconfirmed initialization fixture has no registered run state'
    Remove-Item -LiteralPath $unconfirmedMarker
    Stop-PortablePackage $raceRoot -LockTimeoutSeconds 1
    Check (!(Test-Path -LiteralPath $unconfirmedMarker)) 'confirmed initialization without marker leaves ordinary stop unaffected'
    Write-Output "PASS: $script:count runtime safety assertions (no database or network)"
} finally {
    if ([IO.Path]::GetFullPath($root).StartsWith([IO.Path]::GetTempPath(), [StringComparison]::OrdinalIgnoreCase)) {
        Remove-Item -LiteralPath $root -Recurse -Force
    }
}
