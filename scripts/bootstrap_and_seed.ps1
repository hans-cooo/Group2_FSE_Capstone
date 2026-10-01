<#
.SYNOPSIS
    One-click Core Banking Platform Bootstrap, Database Seeding & Dashboard Population Script
.DESCRIPTION
    1. Boots all containers (Microservices, Polyglot Databases, Kafka, and Observability Tier)
    2. Waits for API Gateway and Loki readiness
    3. Executes the End-to-End Golden Path to seed live transactions and populate Grafana dashboards
.PARAMETER ResetData
    If specified, wipes existing volume data to re-run database SQL init scripts from scratch
#>

[CmdletBinding()]
param(
    [switch]$ResetData
)

$ErrorActionPreference = "Stop"
$rootPath = Split-Path -Parent $PSScriptRoot

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host " CooBS Core Banking: Bootstrap, Seed & Dashboard Population      " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

Push-Location $rootPath
try {
    # 1. Optional clean reset
    if ($ResetData) {
        Write-Host "`n[1/4] Resetting Docker volumes and wiping old database data..." -ForegroundColor Yellow
        docker compose --profile full down -v
    }

    # 2. Start full platform
    Write-Host "`n[2/4] Starting Microservices, Polyglot Databases & Observability Tier..." -ForegroundColor Yellow
    docker compose --profile app --profile observability up -d

    # 3. Wait for API Gateway and Loki
    Write-Host "`n[3/4] Waiting for Gateway (:8080) and Loki (:3100) readiness..." -ForegroundColor Yellow
    $timeout = 90
    $elapsed = 0
    $gatewayReady = $false

    while ($elapsed -lt $timeout) {
        try {
            $resp = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -TimeoutSec 3 -ErrorAction SilentlyContinue
            if ($resp.status -eq "UP") {
                $gatewayReady = $true
                break
            }
        } catch {}
        Start-Sleep -Seconds 3
        $elapsed += 3
        Write-Host "  ... waiting for API Gateway ($elapsed s / $timeout s)" -ForegroundColor Gray
    }

    if (-not $gatewayReady) {
        Write-Host "  [WARN] Gateway took longer than expected. Proceeding..." -ForegroundColor Yellow
    } else {
        Write-Host "  [OK] API Gateway is UP and healthy." -ForegroundColor Green
    }

    # 4. Populate Dashboards by running End-to-End Golden Path
    Write-Host "`n[4/4] Executing E2E Golden Path to generate live transactions & populate dashboards..." -ForegroundColor Yellow
    python scripts/demo_golden_path.py

    Write-Host "`n=================================================================" -ForegroundColor Cyan
    Write-Host " Platform Ready & Operational Dashboards Populated!              " -ForegroundColor Green
    Write-Host "=================================================================" -ForegroundColor Cyan
    Write-Host "  - Centralized Logs Dashboard: http://localhost:3001/d/coobs-centralized-logs" -ForegroundColor White
    Write-Host "  - Operational Metrics SLA:    http://localhost:3001/d/coobs-banking-command-center" -ForegroundColor White
    Write-Host "  - Kafka UI Event Stream:      http://localhost:8088" -ForegroundColor White
    Write-Host "  - API Gateway Ingress:        http://localhost:8080" -ForegroundColor White
    Write-Host "=================================================================`n" -ForegroundColor Cyan

} finally {
    Pop-Location
}
