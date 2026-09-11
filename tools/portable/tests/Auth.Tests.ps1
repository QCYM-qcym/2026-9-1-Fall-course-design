$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot '../runtime/Portable.Common.ps1')
$script:count = 0
function Check($ok, $name) { if (!$ok) { throw "FAIL: $name" }; $script:count++ }
$script:requests = @()
$script:failDictionary = $false
$script:failLogout = $false
$transport = {
    param($Path, $Cookies, $Method, $Body, $Token)
    $script:requests += [pscustomobject]@{ Path=$Path; Cookies=$Cookies; Method=$Method; Body=$Body; Token=$Token }
    switch ($Path) {
        '/api/auth/csrf' { return [pscustomobject]@{code=200;data=[pscustomobject]@{token='test-token';headerName='X-XSRF-TOKEN';parameterName='_csrf'}} }
        '/api/auth/login' { return [pscustomobject]@{code=200;data=[pscustomobject]@{id=1;username='demo_user';role='USER'}} }
        '/api/cities' { if ($script:failDictionary) { throw 'simulated unavailable dictionary' }; return [pscustomobject]@{code=200;data=@([pscustomobject]@{id=1;cityCode='JINAN'})} }
        '/api/auth/logout' { if ($script:failLogout) { throw 'simulated failed logout' }; return [pscustomobject]@{code=200;data=$null} }
        default { throw 'Unexpected path' }
    }
}
Check (Test-AuthenticatedDictionary -Transport $transport) 'readiness requires a successful authenticated dictionary'
Check (($script:requests.Path -join ',') -eq '/api/auth/csrf,/api/auth/login,/api/cities,/api/auth/csrf,/api/auth/logout') 'probe logs in before dictionary and logs out after'
Check ($script:requests[1].Body.loginType -eq 'USER') 'probe uses least privilege USER entrance'
Check ($script:requests[1].Token -eq 'test-token' -and $script:requests[4].Token -eq 'test-token') 'login and logout carry CSRF'
Check ([object]::ReferenceEquals($script:requests[0].Cookies,$script:requests[4].Cookies)) 'one private cookie container shared throughout probe'
$script:requests = @(); $script:failDictionary = $true
try { Test-AuthenticatedDictionary -Transport $transport; throw 'Expected failure' } catch { Check ($_.Exception.Message -match 'unavailable dictionary') 'dictionary failure is not readiness' }
Check ($script:requests[-1].Path -eq '/api/auth/logout') 'failed dictionary still closes authenticated session'
$script:requests = @(); $script:failDictionary = $false; $script:failLogout = $true
try { Test-AuthenticatedDictionary -Transport $transport; throw 'Expected failure' } catch { Check ($_.Exception.Message -match 'failed logout') 'logout failure cannot report readiness' }

$fixture = Join-Path ([IO.Path]::GetTempPath()) ('auth-init-' + [guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path (Join-Path $fixture 'database')
try {
    foreach ($name in @('schema.sql','data.sql','auth-data.sql')) { Write-Utf8 (Join-Path $fixture "database/$name") $name }
    $script:imports = @()
    function Invoke-PrivateClient($Root,$Arguments,$InputSql) { $script:imports += $InputSql }
    Import-DemoDatabase $fixture
    Check (($script:imports -join ',') -eq 'schema.sql,data.sql,auth-data.sql') 'database initialization imports auth after weather, in fixed order'
    function Invoke-PrivateClient($Root,$Arguments,$InputSql) { if ($InputSql -eq 'data.sql') { throw 'import failed' }; $script:imports += $InputSql }
    $script:imports = @()
    try { Import-DemoDatabase $fixture; throw 'Expected failure' } catch { Check ($_.Exception.Message -match 'import failed') 'import failure propagates' }
    Check (($script:imports -join ',') -eq 'schema.sql') 'failed weather import never proceeds to auth import'
} finally {
    $resolved = [IO.Path]::GetFullPath($fixture)
    if (!$resolved.StartsWith([IO.Path]::GetTempPath(),[StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe fixture cleanup path' }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
Write-Output "PASS: $script:count auth portable assertions (simulated HTTP; no live DB)"
