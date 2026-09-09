param([switch]$Offline, [switch]$PackageOnly)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$cache = Join-Path $repo '.portable-build/downloads'
$build = Join-Path $repo ('.portable-build/releases/' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8))
$stage = Join-Path $build 'shandong-weather-demo'
New-Item -ItemType Directory -Force $cache | Out-Null
$lock = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'downloads.lock.json') -Raw | ConvertFrom-Json
foreach ($entry in $lock.PSObject.Properties.Value) {
    $file = Join-Path $cache $entry.file
    if (!(Test-Path -LiteralPath $file)) {
        if ($Offline) { throw "Missing official archive: $($entry.file)" }
        & curl.exe --fail --location --retry 2 --output $file $entry.url
        if ($LASTEXITCODE -ne 0) { throw "Download failed: $($entry.file). No complete ZIP generated." }
    }
    if ((Get-FileHash -LiteralPath $file -Algorithm $entry.algorithm).Hash -ne $entry.checksum) {
        throw "Checksum mismatch: $($entry.file). Preserve for investigation; do not use."
    }
}

# Build from source through Maven resources/repackage, never edit an existing JAR.
if (!$PackageOnly) {
Push-Location (Join-Path $repo 'frontend')
try {
    & npm.cmd test
    if ($LASTEXITCODE -ne 0) { throw 'Frontend tests failed.' }
    & npm.cmd run build
    if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
} finally { Pop-Location }
Push-Location (Join-Path $repo 'backend')
try {
    # Database integration baseline is v1.0; this build runs all non-DB regressions.
    # Actual portable MySQL validation is done against the extracted release, never 3306.
    & mvn.cmd '-Pportable' '-Dtest=!*IntegrationTests,!DatabaseConnectionTests' clean package
    if ($LASTEXITCODE -ne 0) { throw 'Portable Maven package failed.' }
} finally { Pop-Location }

}
New-Item -ItemType Directory $stage | Out-Null
foreach ($dir in @('app','runtime','runtime/scripts','database','licenses','licenses/sources','licenses/frontend','licenses/backend')) {
    New-Item -ItemType Directory (Join-Path $stage $dir) -Force | Out-Null
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$unpack = Join-Path $build 'unpack'
New-Item -ItemType Directory $unpack | Out-Null
foreach ($name in @('java','mysql')) {
    $entry = $lock.$name
    $destination = Join-Path $unpack $name
    [IO.Compression.ZipFile]::ExtractToDirectory((Join-Path $cache $entry.file), $destination)
    $roots = @(Get-ChildItem -LiteralPath $destination -Directory)
    if ($roots.Count -ne 1) { throw "Unexpected archive layout: $name" }
    Move-Item -LiteralPath $roots[0].FullName -Destination (Join-Path $stage "runtime/$name")
}
Copy-Item -LiteralPath (Join-Path $repo 'backend/target/weather-backend-0.3.0-SNAPSHOT.jar') -Destination (Join-Path $stage 'app/weather-demo.jar')
foreach ($sql in @('schema.sql','data.sql')) {
    Copy-Item -LiteralPath (Join-Path $repo "database/$sql") -Destination (Join-Path $stage "database/$sql")
}
Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'runtime') -File | Copy-Item -Destination (Join-Path $stage 'runtime/scripts')
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'README.md') -Destination (Join-Path $stage 'README.md')
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'SOURCES.md') -Destination (Join-Path $stage 'licenses/SOURCES.md')
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'downloads.lock.json') -Destination (Join-Path $stage 'licenses/downloads.lock.json')
Copy-Item -LiteralPath (Join-Path $repo 'frontend/src/assets/maps/README.md') -Destination (Join-Path $stage 'licenses/GeoJSON-README.md')
foreach ($name in @('javaSource','mysqlSource')) {
    Copy-Item -LiteralPath (Join-Path $cache $lock.$name.file) -Destination (Join-Path $stage 'licenses/sources')
}

# Keep original runtime notices; also retain installed frontend dependency notices.
$modules = Join-Path $repo 'frontend/node_modules'
Get-ChildItem -LiteralPath $modules -File -Recurse | Where-Object { $_.Name -match '^(LICENSE|LICENCE|NOTICE|COPYING)(\..*)?$' } | ForEach-Object {
    $relative = $_.FullName.Substring($modules.Length + 1)
    $dest = Join-Path $stage ('licenses/frontend/' + $relative)
    New-Item -ItemType Directory -Force (Split-Path $dest) | Out-Null
    Copy-Item -LiteralPath $_.FullName -Destination $dest
}
# Backend nested JARs retain their original META-INF notices; extract notices for readability.
$jar = [IO.Compression.ZipFile]::OpenRead((Join-Path $stage 'app/weather-demo.jar'))
try {
    foreach ($nested in $jar.Entries | Where-Object FullName -like 'BOOT-INF/lib/*.jar') {
        $memory = New-Object IO.MemoryStream
        $source = $nested.Open()
        try { $source.CopyTo($memory) } finally { $source.Dispose() }
        $memory.Position = 0
        $inner = New-Object IO.Compression.ZipArchive($memory, [IO.Compression.ZipArchiveMode]::Read)
        try {
            foreach ($notice in $inner.Entries | Where-Object { $_.FullName -match '(?i)(^|/)(LICENSE|LICENCE|NOTICE|COPYING)(\..*)?$' }) {
                $dest = Join-Path $stage ('licenses/backend/' + $nested.Name + '/' + $notice.FullName)
                New-Item -ItemType Directory -Force (Split-Path $dest) | Out-Null
                [IO.Compression.ZipFileExtensions]::ExtractToFile($notice, $dest, $true)
            }
        } finally { $inner.Dispose(); $memory.Dispose() }
    }
} finally { $jar.Dispose() }

# CMD file names can be localized, contents stay ASCII for Windows code-page independence.
foreach ($item in @(@('启动演示.cmd','Start-Demo.ps1'),@('停止演示.cmd','Stop-Demo.ps1'))) {
    $body = "@echo off`r`nsetlocal`r`npowershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File `"%~dp0runtime\scripts\$($item[1])`"`r`nif errorlevel 1 pause`r`nendlocal`r`n"
    [IO.File]::WriteAllText((Join-Path $stage $item[0]), $body, [Text.Encoding]::ASCII)
}
if (Test-Path -LiteralPath (Join-Path $stage 'var')) { throw 'Release must not contain runtime state.' }
$zip = Join-Path $build 'shandong-weather-demo-v1.0-win-x64.zip'
[IO.Compression.ZipFile]::CreateFromDirectory($stage, $zip, [IO.Compression.CompressionLevel]::Optimal, $true)
$hash = (Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLowerInvariant()
[IO.File]::WriteAllText(($zip + '.sha256'), "$hash  $([IO.Path]::GetFileName($zip))`n", [Text.Encoding]::ASCII)
Write-Output "ZIP: $zip"
Write-Output "SHA256: $hash"
