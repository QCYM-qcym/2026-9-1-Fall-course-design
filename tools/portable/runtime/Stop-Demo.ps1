param()
. (Join-Path $PSScriptRoot 'Portable.Common.ps1')
try {
    Stop-PortablePackage (Get-PackageRoot $PSScriptRoot)
    Write-Host 'Demo stopped (or no saved run). No force termination was used.'
} catch {
    Write-Host ('Stop refused or incomplete: ' + $_.Exception.Message) -ForegroundColor Red
    exit 1
}
