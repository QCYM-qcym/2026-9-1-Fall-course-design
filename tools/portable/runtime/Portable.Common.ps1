Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'

function Get-PackageRoot([string]$ScriptsDirectory) {
    [IO.Path]::GetFullPath((Join-Path $ScriptsDirectory '../..')).TrimEnd('\')
}
function New-LocalSecret {
    $bytes = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes); ([BitConverter]::ToString($bytes)).Replace('-','').ToLowerInvariant() }
    finally { $rng.Dispose() }
}
function ConvertTo-NativeArgument([string]$Value) {
    '"' + [regex]::Replace([regex]::Replace($Value, '(\\*)"', '$1$1\"'), '(\\+)$', '$1$1') + '"'
}
function Write-Utf8([string]$Path, [string]$Content) {
    [IO.File]::WriteAllText($Path, $Content, (New-Object Text.UTF8Encoding($false)))
}
function Write-MySqlConfig([string]$Path, [string]$Content, [Text.Encoding]$Encoding = [Text.Encoding]::Default) {
    # MySQL 8 Windows filesystem options use the active ANSI code page. Java
    # properties and SQL input retain their separate Unicode/UTF-8 handling.
    $strict = [Text.Encoding]::GetEncoding($Encoding.CodePage,
        [Text.EncoderFallback]::ExceptionFallback, [Text.DecoderFallback]::ExceptionFallback)
    try {
        $bytes = $strict.GetBytes($Content)
        if ($strict.GetString($bytes) -cne $Content) { throw 'Encoding round-trip mismatch.' }
    } catch {
        throw 'Package path cannot be represented in the Windows ANSI code page. Use a representable folder name; no MySQL configuration was written.'
    }
    [IO.File]::WriteAllBytes($Path, $bytes)
}
function Get-DataPhase([string]$Root) {
    if (Test-Path -LiteralPath (Join-Path $Root 'var/initialization-exit-unconfirmed')) {
        throw 'Initialization exit is unconfirmed. Preserve var; automatic initialization or restart is forbidden.'
    }
    $data = Join-Path $Root 'var/mysql'
    $marker = Join-Path $Root 'var/initialized'
    if (!(Test-Path -LiteralPath $data) -and !(Test-Path -LiteralPath $marker)) {
        foreach ($pending in @('var/initialization.started','var/private/root.cnf','var/private/app.secret')) {
            if (Test-Path -LiteralPath (Join-Path $Root $pending)) { throw 'Interrupted initialization; preserve var. Automatic retry is disabled.' }
        }
        return 'Fresh'
    }
    foreach ($item in @('var/initialized','var/mysql/auto.cnf','var/mysql/mysql','var/private/root.cnf','var/private/app.secret')) {
        if (!(Test-Path -LiteralPath (Join-Path $Root $item))) { throw 'Ambiguous or incomplete initialization. Preserve var; do not delete/reinitialize. Inspect private package state.' }
    }
    if (([IO.File]::ReadAllText($marker)).Trim() -ne 'complete') { throw 'Invalid initialization marker; preserve var.' }
    'Ready'
}
function Get-ServerConfig([string]$Root) {
    $base = (Join-Path $Root 'runtime/mysql').Replace('\','/')
    $data = (Join-Path $Root 'var/mysql').Replace('\','/')
    $log = (Join-Path $Root 'var/logs/mysql-error.log').Replace('\','/')
    $pidFile = (Join-Path $Root 'var/mysql.pid').Replace('\','/')
    @"
[mysqld]
basedir="$base"
datadir="$data"
port=13306
bind-address=127.0.0.1
mysqlx=0
skip-name-resolve=ON
persisted-globals-load=OFF
local-infile=OFF
general-log=OFF
slow-query-log=OFF
skip-log-bin
log-error="$log"
pid-file="$pidFile"
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
"@
}
function Get-AppConfig([string]$Password, [string]$StopFile = '') {
    # Java properties are ISO-8859-1 unless loaded through a Reader: escape Unicode.
    $stop = ConvertTo-PropertyValue ($StopFile.Replace('\','/'))
    @"
spring.datasource.url=jdbc:mysql://127.0.0.1:13306/shandong_weather?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=weather_demo
spring.datasource.password=$Password
spring.sql.init.mode=never
server.address=127.0.0.1
server.port=18080
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
weather.portable.stop-file=$stop
"@
}
function ConvertTo-PropertyValue([string]$Value) {
    $result = New-Object Text.StringBuilder
    foreach ($c in $Value.ToCharArray()) {
        if ([int]$c -gt 126 -or [int]$c -lt 32) { $null = $result.Append(('\u{0:x4}' -f [int]$c)) }
        elseif ($c -eq '\') { $null = $result.Append('\\') }
        else { $null = $result.Append($c) }
    }
    $result.ToString()
}
function Get-JavaArguments([string]$Root, [string]$StopFile) {
    @('-jar', (Join-Path $Root 'app/weather-demo.jar'),
      ('--spring.config.additional-location=' + ([uri](Join-Path $Root 'var/private/application.properties')).AbsoluteUri),
      '--spring.datasource.url=jdbc:mysql://127.0.0.1:13306/shandong_weather?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true',
      '--spring.datasource.username=weather_demo', '--spring.sql.init.mode=never',
      '--server.address=127.0.0.1', '--server.port=18080', '--server.shutdown=graceful',
      '--spring.lifecycle.timeout-per-shutdown-phase=30s',
      ('--weather.portable.stop-file=' + $StopFile.Replace('\','/')))
}
function Get-ClientConfig([string]$Password) {
    "[client]`nhost=127.0.0.1`nport=13306`nprotocol=TCP`nuser=root`npassword=$Password`ndefault-character-set=utf8mb4`nconnect-timeout=5`n"
}
function Get-MySqlServerArguments([string]$Root, [switch]$Bootstrap) {
    # MySQL 8.0.46 restart_monitor_win.cc converts wide argv using a buffer sized
    # by wchar count, truncating multibyte paths. --no-monitor is an early option
    # that bypasses that respawn path; our supervisor then owns the real DB PID.
    '--defaults-file=' + (Join-Path $Root 'var/private/my.ini')
    '--no-monitor'
    if ($Bootstrap) {
        '--init-file=' + (Join-Path $Root 'var/private/bootstrap.sql')
        '--log-error=' + (Join-Path $Root 'var/private/mysql-bootstrap-error.log')
    }
}
function Get-BootstrapSql([string]$RootPassword, [string]$AppPassword) {
    if ($RootPassword -notmatch '^[a-f0-9]{64}$' -or $AppPassword -notmatch '^[a-f0-9]{64}$') { throw 'Invalid generated credential format.' }
    @"
ALTER USER 'root'@'localhost' IDENTIFIED BY '$RootPassword';
CREATE USER 'root'@'127.0.0.1' IDENTIFIED BY '$RootPassword';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1' WITH GRANT OPTION;
CREATE USER 'weather_demo'@'127.0.0.1' IDENTIFIED BY '$AppPassword';
GRANT SELECT, INSERT, UPDATE, DELETE ON shandong_weather.* TO 'weather_demo'@'127.0.0.1';
"@
}
function Test-ProcessIdentity($Expected, $Actual) {
    if ($null -eq $Expected -or $null -eq $Actual) { return $false }
    foreach ($name in @('Id','Exe','CommandLine','StartTicks')) {
        if ([string]::IsNullOrWhiteSpace([string]$Expected.$name) -or [string]$Expected.$name -cne [string]$Actual.$name) { return $false }
    }
    $true
}
function Get-ProcessIdentity([int]$ProcessId) {
    $w = Get-CimInstance Win32_Process -Filter "ProcessId=$ProcessId"
    if (!$w) { return $null }
    [pscustomobject]@{ Id=[int]$w.ProcessId; Exe=[string]$w.ExecutablePath; CommandLine=[string]$w.CommandLine; StartTicks=[string]$w.CreationDate.ToUniversalTime().Ticks }
}
function Assert-OwnedProcess($Root, $Identity, [ValidateSet('App','Database')]$Kind) {
    $exe = if ($Kind -eq 'App') { Join-Path $Root 'runtime/java/bin/java.exe' } else { Join-Path $Root 'runtime/mysql/bin/mysqld.exe' }
    $argument = if ($Kind -eq 'App') { Join-Path $Root 'app/weather-demo.jar' } else { '--defaults-file=' + (Join-Path $Root 'var/private/my.ini') }
    if ($Identity.Exe -ine $exe -or !$Identity.CommandLine.Contains((ConvertTo-NativeArgument $argument))) { throw "Saved $Kind identity is not this package. No process was stopped." }
    $actual = Get-ProcessIdentity $Identity.Id
    if ($null -eq $actual) { return $false }
    if (!(Test-ProcessIdentity $Identity $actual)) { throw "Process identity mismatch ($Kind). No process was stopped." }
    $true
}
function Test-DictionaryResponse([string]$Body) {
    try {
        $r = ConvertFrom-Json $Body
        $r.code -eq 200 -and @($r.data).Count -gt 0 -and $null -ne $r.data[0].id -and ![string]::IsNullOrWhiteSpace($r.data[0].cityCode)
    } catch { $false }
}
function Get-InitializationFiles { @('schema.sql','data.sql','auth-data.sql') }
function Import-DemoDatabase([string]$Root) {
    foreach ($sql in (Get-InitializationFiles)) {
        Invoke-PrivateClient $Root @('--batch') ([IO.File]::ReadAllText((Join-Path $Root "database/$sql"),[Text.Encoding]::UTF8))
    }
}
function Invoke-DemoJson([string]$Path, [Net.CookieContainer]$Cookies, [string]$Method = 'GET', $Body = $null, [string]$Token = '') {
    if ($Path -notin @('/api/auth/csrf','/api/auth/login','/api/auth/logout','/api/cities')) { throw 'Unexpected startup probe path.' }
    $request = [Net.HttpWebRequest]::Create('http://127.0.0.1:18080' + $Path)
    $request.Proxy = $null; $request.Timeout = 3000; $request.ReadWriteTimeout = 3000; $request.AllowAutoRedirect = $false
    $request.CookieContainer = $Cookies; $request.Method = $Method
    if ($Token) { $request.Headers['X-XSRF-TOKEN'] = $Token }
    if ($null -ne $Body) {
        $bytes = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Compress))
        $request.ContentType = 'application/json'; $request.ContentLength = $bytes.Length
        $stream = $request.GetRequestStream()
        try { $stream.Write($bytes,0,$bytes.Length) } finally { $stream.Dispose() }
    }
    $response = $request.GetResponse()
    try {
        $reader = New-Object IO.StreamReader($response.GetResponseStream(),[Text.Encoding]::UTF8)
        try { $result = ConvertFrom-Json $reader.ReadToEnd() } finally { $reader.Dispose() }
        if ([int]$response.StatusCode -ne 200 -or $result.code -ne 200) { throw 'Startup probe was not successful.' }
        return $result
    } finally { $response.Dispose() }
}
function Test-AuthenticatedDictionary([scriptblock]$Transport = ${function:Invoke-DemoJson}) {
    # Public DEMO ONLY credentials, unrelated to the random MySQL connection secret.
    # The cookie jar stays in memory and is never handed to the browser or written.
    $cookies = New-Object Net.CookieContainer
    $loggedIn = $false
    try {
        $csrf = & $Transport '/api/auth/csrf' $cookies 'GET' $null ''
        $login = & $Transport '/api/auth/login' $cookies 'POST' @{username='demo_user';password='DemoUser@2026';loginType='USER'} $csrf.data.token
        $loggedIn = $true
        if ($login.code -ne 200 -or $login.data.role -ne 'USER') { throw 'Unexpected startup probe identity.' }
        $dictionary = & $Transport '/api/cities' $cookies 'GET' $null ''
        return (Test-DictionaryResponse ($dictionary | ConvertTo-Json -Depth 8 -Compress))
    } finally {
        if ($loggedIn) {
            $csrf = & $Transport '/api/auth/csrf' $cookies 'GET' $null ''
            $logout = & $Transport '/api/auth/logout' $cookies 'POST' @{} $csrf.data.token
            if ($logout.code -ne 200) { throw 'Startup probe logout failed.' }
        }
    }
}
function Assert-PortsFree {
    $used = @([Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners() | Where-Object { $_.Port -in @(13306,18080) })
    if ($used.Count) { throw ('Required port already occupied: ' + (($used | ForEach-Object { $_.Port } | Select-Object -Unique) -join ', ') + '. Nothing was stopped.') }
}
function Test-ListenerIdentity($Listeners, [int]$Port, [int]$ProcessId) {
    $matches = @($Listeners | Where-Object { $_.Port -eq $Port })
    $matches.Count -eq 1 -and $matches[0].Address -eq '127.0.0.1' -and $matches[0].Id -eq $ProcessId
}
function Initialize-PortableNativeTcp {
    if ('PortableNativeTcp' -as [type]) { return }
    Add-Type -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Net;
using System.Runtime.InteropServices;
public static class PortableNativeTcp {
    [DllImport("iphlpapi.dll", SetLastError=true)]
    private static extern uint GetExtendedTcpTable(IntPtr table, ref int size, bool order, int family, int tableClass, uint reserved);
    public sealed class Listener { public int Port; public string Address; public int Id; }
    public static Listener[] Read() {
        int size=0;
        uint code=GetExtendedTcpTable(IntPtr.Zero, ref size, false, 2, 3, 0);
        if(code!=122 && code!=0) throw new InvalidOperationException("Cannot inspect TCP owners.");
        for(int attempt=0; attempt<3; attempt++) {
            IntPtr buffer=Marshal.AllocHGlobal(size);
            try {
                code=GetExtendedTcpTable(buffer, ref size, false, 2, 3, 0);
                if(code==122) continue;
                if(code!=0) throw new InvalidOperationException("Cannot inspect TCP owners.");
                int count=Marshal.ReadInt32(buffer);
                var result=new List<Listener>();
                for(int i=0;i<count;i++) {
                    IntPtr row=IntPtr.Add(buffer,4+i*24);
                    if(Marshal.ReadInt32(row)!=2) continue;
                    byte[] port=BitConverter.GetBytes(Marshal.ReadInt32(row,8));
                    result.Add(new Listener { Port=(port[0]<<8)|port[1], Address=new IPAddress((long)(uint)Marshal.ReadInt32(row,4)).ToString(), Id=Marshal.ReadInt32(row,20) });
                }
                return result.ToArray();
            } finally { Marshal.FreeHGlobal(buffer); }
        }
        throw new InvalidOperationException("TCP owner table changed repeatedly.");
    }
}
'@
}
function Assert-OwnedListener([int]$Port, [int]$ProcessId) {
    Initialize-PortableNativeTcp
    if (!(Test-ListenerIdentity ([PortableNativeTcp]::Read()) $Port $ProcessId)) { throw "Port $Port is not exclusively owned on 127.0.0.1 by the package process." }
}
function Assert-PortablePrerequisites([string]$Root) {
    if (![Environment]::Is64BitOperatingSystem -or ![Environment]::Is64BitProcess) { throw 'Use Windows x64 and 64-bit Windows PowerShell 5.1.' }
    foreach ($file in @('app/weather-demo.jar','runtime/java/bin/java.exe','runtime/mysql/bin/mysqld.exe','runtime/mysql/bin/mysql.exe','runtime/mysql/bin/mysqladmin.exe','database/schema.sql','database/data.sql','database/auth-data.sql')) {
        if (!(Test-Path -LiteralPath (Join-Path $Root $file) -PathType Leaf)) { throw "Package file missing: $file" }
    }
    foreach ($dll in @('vcruntime140.dll','vcruntime140_1.dll','msvcp140.dll')) {
        if (!(Test-Path -LiteralPath (Join-Path $env:WINDIR "System32/$dll")) -and !(Test-Path -LiteralPath (Join-Path $Root "runtime/mysql/bin/$dll"))) {
            throw "Microsoft Visual C++ 2015-2022 x64 runtime missing ($dll). Install separately before running this package; no installation was attempted."
        }
    }
}
function Protect-PrivateDirectory([string]$Path) {
    $null = New-Item -ItemType Directory -Path $Path -Force
    $acl = New-Object Security.AccessControl.DirectorySecurity
    $acl.SetAccessRuleProtection($true,$false)
    $sid = [Security.Principal.WindowsIdentity]::GetCurrent().User
    foreach ($principal in @($sid, (New-Object Security.Principal.SecurityIdentifier('S-1-5-18')))) {
        $rule = New-Object Security.AccessControl.FileSystemAccessRule($principal,'FullControl','ContainerInherit,ObjectInherit','None','Allow')
        $acl.AddAccessRule($rule)
    }
    # Persist only the DACL section we changed. Set-Acl can also request
    # the audit section and require SeSecurityPrivilege on subsequent runs.
    [IO.Directory]::SetAccessControl($Path, $acl)
}
function Assert-NoReparsePath([string]$Root) {
    # Reject junctions/symlinks before any write to runtime-owned state.
    $path = [IO.DirectoryInfo]$Root
    while ($null -ne $path) {
        if ($path.Exists -and ($path.Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw 'Package path must not contain junctions or symlinks.' }
        $path = $path.Parent
    }
    $varPath = Join-Path $Root 'var'
    if (Test-Path -LiteralPath $varPath) {
        $queue = New-Object 'Collections.Generic.Queue[string]'; $queue.Enqueue($varPath)
        while ($queue.Count) {
            $item = Get-Item -LiteralPath $queue.Dequeue() -Force
            if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'var contains a junction or symlink; preserve and inspect it.' }
            if ($item.PSIsContainer) { foreach ($child in Get-ChildItem -LiteralPath $item.FullName -Force) { $queue.Enqueue($child.FullName) } }
        }
    }
}
function New-ChildStartInfo([string]$Exe, [string[]]$Arguments, [string]$Root, [hashtable]$Environment = @{}) {
    $info = New-Object Diagnostics.ProcessStartInfo
    $info.FileName = $Exe
    $info.Arguments = ($Arguments | ForEach-Object { ConvertTo-NativeArgument $_ }) -join ' '
    $info.WorkingDirectory = $Root
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardInput = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    # Remove inherited development passwords and higher-priority Spring/JVM overrides.
    foreach ($key in @($info.EnvironmentVariables.Keys)) {
        if ($key -match '^(DB_|SPRING_|SERVER_|WEATHER_|MYSQL|JAVA_TOOL_OPTIONS$|JDK_JAVA_OPTIONS$|_JAVA_OPTIONS$|JAVA_OPTS$)') { $info.EnvironmentVariables.Remove($key) }
    }
    foreach ($key in $Environment.Keys) { $info.EnvironmentVariables[$key] = [string]$Environment[$key] }
    $info
}
function Start-LoggedChild($Info, [string]$LogPrefix) {
    $p = New-Object Diagnostics.Process; $p.StartInfo = $Info
    $out = [IO.File]::Open($LogPrefix + '.out.log','Create','Write','Read')
    $err = [IO.File]::Open($LogPrefix + '.err.log','Create','Write','Read')
    try {
        $null = $p.Start()
        $outTask = $p.StandardOutput.BaseStream.CopyToAsync($out)
        $errTask = $p.StandardError.BaseStream.CopyToAsync($err)
        [pscustomobject]@{ Process=$p; Out=$out; Err=$err; OutTask=$outTask; ErrTask=$errTask }
    } catch { $out.Dispose(); $err.Dispose(); throw }
}
function Close-ChildLogs($Child) {
    if ($Child -and $Child.Process.HasExited) {
        $null = $Child.OutTask.GetAwaiter().GetResult()
        $null = $Child.ErrTask.GetAwaiter().GetResult()
        $Child.Out.Dispose(); $Child.Err.Dispose(); $Child.Process.Dispose()
    }
}
function New-PrivateClientStartInfo([string]$Root, [string[]]$Arguments, [switch]$Admin) {
    $name = if ($Admin) { 'mysqladmin.exe' } else { 'mysql.exe' }
    # Windows mysql clients can misdecode Unicode option paths. The fixed child
    # working directory makes this ASCII relative path refer only to this package.
    # mysys/my_default.cc in 8.0.46 checks MYSQL_TEST_LOGIN_FILE before APPDATA.
    # An absent explicit file suppresses development .mylogin.cnf lookup, without
    # the --no-login-paths option that these bundled Windows clients reject.
    $disabledLogin = 'var/private/disabled-login.cnf'
    if (Test-Path -LiteralPath (Join-Path $Root $disabledLogin)) {
        throw 'The disabled login-file path already exists. Preserve and inspect it; no MySQL client was started.'
    }
    $argsList = @('--defaults-file=var/private/root.cnf') + $Arguments
    New-ChildStartInfo (Join-Path $Root "runtime/mysql/bin/$name") $argsList $Root @{ MYSQL_TEST_LOGIN_FILE=$disabledLogin }
}
function Invoke-PrivateClient([string]$Root, [string[]]$Arguments, [string]$InputSql = '', [switch]$Admin) {
    $info = New-PrivateClientStartInfo $Root $Arguments -Admin:$Admin
    $p = New-Object Diagnostics.Process; $p.StartInfo = $info
    try {
        $null = $p.Start()
        $stdout = $p.StandardOutput.ReadToEndAsync(); $stderr = $p.StandardError.ReadToEndAsync()
        if ($InputSql) {
            $bytes = [Text.Encoding]::UTF8.GetBytes($InputSql)
            $p.StandardInput.BaseStream.Write($bytes,0,$bytes.Length)
            $p.StandardInput.BaseStream.Flush()
        }
        $p.StandardInput.Close()
        if (!$p.WaitForExit(60000)) { throw 'Private MySQL client timed out; preserve state and inspect processes. No force stop performed.' }
        $null = $stdout.GetAwaiter().GetResult(); $null = $stderr.GetAwaiter().GetResult()
        # Never surface client stderr: SQL errors may echo passwords.
        if ($p.ExitCode -ne 0) { throw 'Private MySQL command failed. Credentials and SQL were withheld.' }
    } finally { $p.Dispose() }
}
function Save-RunState([string]$Root, $State) {
    $path = Join-Path $Root 'var/run-state.json'
    $temp = Join-Path $Root 'var/run-state.tmp'
    Write-Utf8 $temp ($State | ConvertTo-Json -Depth 5)
    if (Test-Path -LiteralPath $path) { [IO.File]::Replace($temp,$path,[NullString]::Value) }
    else { [IO.File]::Move($temp,$path) }
}
function Get-ExitObservation($Expected, $Actual) {
    if ($null -eq $Actual) { return 'Exited' }
    $incomplete = $false
    foreach ($name in @('Id','Exe','CommandLine','StartTicks')) {
        if ([string]::IsNullOrWhiteSpace([string]$Actual.$name)) { $incomplete = $true; continue }
        if ([string]$Expected.$name -cne [string]$Actual.$name) {
            throw "Process identity changed while waiting for exit (field: $name). No further stop action was performed."
        }
    }
    if ($incomplete) { return 'Exiting' }
    'Running'
}
function Wait-OwnedExit([string]$Root, $Identity, [string]$Kind, [int]$Seconds = 60) {
    $end = [DateTime]::UtcNow.AddSeconds($Seconds)
    # The stop action already passed strict ownership validation. This loop only
    # observes: Win32_Process can transiently omit fields while a process exits.
    while ((Get-ExitObservation $Identity (Get-ProcessIdentity $Identity.Id)) -ne 'Exited') {
        if ([DateTime]::UtcNow -gt $end) { throw "$Kind did not exit gracefully. State retained; no force stop performed." }
        Start-Sleep -Milliseconds 250
    }
}
function Enter-LifecycleLock([string]$Root, [ValidateRange(1,600)][int]$TimeoutSeconds = 130) {
    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    while ($true) {
        try { return [IO.File]::Open((Join-Path $Root 'var/stop.lock'),'OpenOrCreate','ReadWrite','None') }
        catch [IO.IOException] {
            if ([DateTime]::UtcNow -gt $deadline) { throw 'Startup or stop operation is still active. No successful stop was confirmed.' }
            Start-Sleep -Milliseconds 250
        }
    }
}
function Stop-PortablePackage([string]$Root, [ValidateRange(1,600)][int]$LockTimeoutSeconds = 130) {
    Assert-NoReparsePath $Root
    if (!(Test-Path -LiteralPath (Join-Path $Root 'var'))) { return }
    $path = Join-Path $Root 'var/run-state.json'
    # Startup holds this same lock before initialization and process registration.
    # Absence of state is meaningful only after acquiring the lifecycle lock.
    $stopLock = Enter-LifecycleLock $Root $LockTimeoutSeconds
    try {
        if (Test-Path -LiteralPath (Join-Path $Root 'var/initialization-exit-unconfirmed')) {
            throw 'Initialization exit is unconfirmed. Preserve the scene; successful stop has NOT been confirmed.'
        }
        if (!(Test-Path -LiteralPath $path)) { return }
        $state = ConvertFrom-Json ([IO.File]::ReadAllText($path))
        if ($state.Root -cne $Root) { throw 'Package moved while running or invalid state. No process was stopped.' }
        $validStop = Join-Path $Root ('var/private/' + $state.RunId + '.stop')
        if ($state.RunId -notmatch '^[a-f0-9]{32}$' -or $state.StopFile -cne $validStop) { throw 'Invalid stop-file state.' }
        # Validate both identities before any stop operation, including a DB request.
        $appAlive = $false; $dbAlive = $false
        if ($state.App) { $appAlive = Assert-OwnedProcess $Root $state.App App }
        if ($state.Database) { $dbAlive = Assert-OwnedProcess $Root $state.Database Database }
        if ($appAlive) {
            $null = New-Item -ItemType File -Path $validStop -Force
            Wait-OwnedExit $Root $state.App App
        }
        if ($dbAlive -and (Assert-OwnedProcess $Root $state.Database Database)) {
            Assert-OwnedListener 13306 $state.Database.Id
            Invoke-PrivateClient $Root @('shutdown') -Admin
            Wait-OwnedExit $Root $state.Database Database
        }
        Remove-Item -LiteralPath $path -Force
        if (Test-Path -LiteralPath $validStop) { Remove-Item -LiteralPath $validStop -Force }
    } finally { $stopLock.Dispose() }
}
