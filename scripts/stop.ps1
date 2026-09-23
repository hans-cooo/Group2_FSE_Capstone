<#
.SYNOPSIS
    Stops local data foundation containers
#>
$rootPath = Split-Path -Parent $PSScriptRoot
Push-Location $rootPath
try {
    docker compose stop
    Write-Host "Data foundation containers stopped." -ForegroundColor Yellow
} finally {
    Pop-Location
}
