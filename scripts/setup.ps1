<#
.SYNOPSIS
    One-click Setup & Bootstrap Script for Local Data Foundation (Phase 1)
.DESCRIPTION
    Validates Docker prerequisites, copies .env, boots the 4 datastores + Kafka UI,
    waits for database healthiness, and runs connectivity verification.
#>

[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host " Core Retail Ledger - Phase 1: Local Data Foundation Setup       " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

# 1. Check Docker prerequisite
Write-Host "[1/5] Checking Docker and Docker Compose..." -ForegroundColor Yellow
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "Docker is not installed or not in PATH. Please install Docker Desktop and start it."
    exit 1
}

try {
    $dockerInfo = docker info 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Docker daemon is not running. Please start Docker Desktop and retry."
        exit 1
    }
} catch {
    Write-Error "Failed to connect to Docker daemon: $_"
    exit 1
}

# 2. Check and copy .env file
Write-Host "[2/5] Checking environment configuration (.env)..." -ForegroundColor Yellow
$rootPath = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $rootPath ".env"
$envExample = Join-Path $rootPath ".env.example"

if (-not (Test-Path $envFile)) {
    Write-Host "  .env not found. Copying from .env.example..." -ForegroundColor Gray
    Copy-Item $envExample $envFile
    Write-Host "  Created active .env file." -ForegroundColor Green
} else {
    Write-Host "  Existing .env found." -ForegroundColor Green
}

# Load .env into process environment
Get-Content $envFile | ForEach-Object {
    $trimmed = $_.Trim()
    if ($trimmed -and -not $trimmed.StartsWith("#") -and $trimmed.Contains("=")) {
        $kv = $trimmed.Split("=", 2)
        [System.Environment]::SetEnvironmentVariable($kv[0].Trim(), $kv[1].Trim(), "Process")
    }
}
$appUser = if ($env:APP_USER) { $env:APP_USER } else { "core_user" }
$appUserPassword = if ($env:APP_USER_PASSWORD) { $env:APP_USER_PASSWORD } else { "" }
$oracleDb = if ($env:ORACLE_DATABASE) { $env:ORACLE_DATABASE } else { "XEPDB1" }

# 3. Spin up docker-compose services
Write-Host "[3/5] Starting Data Foundation containers..." -ForegroundColor Yellow
Push-Location $rootPath
try {
    docker compose up -d
    if ($LASTEXITCODE -ne 0) {
        Write-Error "docker compose up failed with exit code $LASTEXITCODE"
    }
} finally {
    Pop-Location
}

# 4. Wait for services to become healthy
Write-Host "[4/5] Waiting for datastores to become healthy (this may take ~45-60s on initial cold boot)..." -ForegroundColor Yellow

$maxAttempts = 40
$attempt = 1
$allHealthy = $false

while ($attempt -le $maxAttempts) {
    Start-Sleep -Seconds 3
    $containers = docker compose -f (Join-Path $rootPath "docker-compose.yml") ps --format json | ConvertFrom-Json
    
    $oracleHealthy = $false
    $postgresHealthy = $false
    $redisHealthy = $false
    $kafkaHealthy = $false

    foreach ($c in $containers) {
        $name = $c.Name
        $status = $c.Health
        if ($name -match "oracle-core-db" -and ($status -eq "healthy" -or $c.State -eq "running")) { $oracleHealthy = $true }
        if ($name -match "postgres-audit-db" -and ($status -eq "healthy" -or $c.State -eq "running")) { $postgresHealthy = $true }
        if ($name -match "redis-cache" -and ($status -eq "healthy" -or $c.State -eq "running")) { $redisHealthy = $true }
        if ($name -match "kafka-broker" -and ($status -eq "healthy" -or $c.State -eq "running")) { $kafkaHealthy = $true }
    }

    Write-Host "  [Attempt $attempt/$maxAttempts] Status -> Oracle: $oracleHealthy | Postgres: $postgresHealthy | Redis: $redisHealthy | Kafka: $kafkaHealthy" -ForegroundColor Gray

    # Test actual connectivity to Oracle & Postgres using loaded env vars
    $oraTest = docker exec oracle-core-db bash -c "printf 'SELECT 1 FROM DUAL;\n' | sqlplus -s $appUser/$appUserPassword@localhost:1521/$oracleDb" 2>&1
    $pgTest = docker exec postgres-audit-db bash -c 'pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}' 2>&1

    if ($LASTEXITCODE -eq 0 -and $oraTest -match "1" -and $pgTest -match "accepting connections") {
        $allHealthy = $true
        break
    }

    $attempt++
}

if (-not $allHealthy) {
    Write-Warning "Datastores took longer than expected to initialize. Running verification to inspect detailed statuses..."
} else {
    Write-Host "All core datastores initialized successfully!" -ForegroundColor Green
}

# 5. Run Verification Script
Write-Host "[5/5] Executing verification checks..." -ForegroundColor Yellow
$verifyScript = Join-Path $PSScriptRoot "verify.ps1"
& $verifyScript

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host " Local Data Foundation is Ready for Development!                 " -ForegroundColor Green
Write-Host " Kafka UI: http://localhost:8085                                 " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan
