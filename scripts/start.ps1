<#
.SYNOPSIS
    Starts local data foundation containers
#>
$rootPath = Split-Path -Parent $PSScriptRoot
Push-Location $rootPath
try {
    docker compose up -d
    Write-Host "Data foundation containers started." -ForegroundColor Green
} finally {
    Pop-Location
}
