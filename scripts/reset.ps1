<#
.SYNOPSIS
    Tears down all containers and wipes volumes for a clean reset
#>
$rootPath = Split-Path -Parent $PSScriptRoot
Write-Host "Resetting Local Data Foundation (wiping volumes & fresh re-initialization)..." -ForegroundColor Red
Push-Location $rootPath
try {
    docker compose down -v
    Write-Host "Volumes removed. Running setup.ps1 to bootstrap fresh database state..." -ForegroundColor Yellow
    & (Join-Path $PSScriptRoot "setup.ps1")
} finally {
    Pop-Location
}
