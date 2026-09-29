<#
.SYNOPSIS
    Automated Headless & GUI Runner for CooBS Core Banking JMeter Load Test Suite.

.DESCRIPTION
    Executes test/jmeter-load-test.jmx in non-GUI CLI mode or launches the JMeter GUI.
    Generates an interactive HTML dashboard report with latency percentiles, throughput, and error analysis.

.PARAMETER HostName
    API Gateway host. Default: localhost.

.PARAMETER Port
    API Gateway port. Default: 8080.

.PARAMETER Threads
    Number of concurrent virtual user threads. Default: 5.

.PARAMETER RampUp
    Thread ramp-up time in seconds. Default: 10.

.PARAMETER Duration
    Test duration in seconds. Default: 60.

.PARAMETER Amount
    Ping-pong transfer nominal amount in PHP. Default: 1.00.

.PARAMETER SlaMs
    SLA response time assertion threshold in milliseconds. Default: 3000.

.PARAMETER DelayMs
    Gaussian Random Timer pacing delay between requests in milliseconds. Default: 300.

.PARAMETER Clean
    Wipe prior test results (JTL log and HTML report) before running. Default: true.

.PARAMETER Report
    Automatically open the generated HTML dashboard report in the default browser upon test completion.

.PARAMETER Gui
    Launch the JMeter graphical user interface instead of running in headless CLI mode.

.EXAMPLE
    .\scripts\run_jmeter_test.ps1
    Runs the standard 60-second test with 5 virtual users and generates an HTML report.

.EXAMPLE
    .\scripts\run_jmeter_test.ps1 -Threads 10 -Duration 120 -Report
    Runs 10 concurrent threads for 2 minutes and launches the HTML report in the browser.

.EXAMPLE
    .\scripts\run_jmeter_test.ps1 -Gui
    Launches JMeter GUI loaded with the CooBS test plan.
#>
[CmdletBinding()]
param (
    [string]$HostName = "localhost",
    [int]$Port = 8080,
    [int]$Threads = 5,
    [int]$RampUp = 10,
    [int]$Duration = 60,
    [decimal]$Amount = 1.00,
    [int]$SlaMs = 3000,
    [int]$DelayMs = 300,
    [switch]$Clean = $true,
    [switch]$Report,
    [switch]$Gui
)

$ErrorActionPreference = "Stop"

# 1. Resolve repository paths
$repoRoot = Split-Path -Parent $PSScriptRoot
$jmxPath = Join-Path $repoRoot "test\jmeter-load-test.jmx"
$resultsDir = Join-Path $repoRoot "test\results"
$jtlPath = Join-Path $resultsDir "results.jtl"
$reportDir = Join-Path $resultsDir "html-report"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "  CooBS Core Retail Banking - JMeter Load Test Automation        " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

# 2. Check JMeter availability
$jmeterCmd = Get-Command "jmeter" -ErrorAction SilentlyContinue
if (-not $jmeterCmd) {
    Write-Error "Apache JMeter executable ('jmeter') was not found in PATH. Please install JMeter and ensure it is in your system PATH."
    exit 1
}

# 3. Check API Gateway connectivity
$healthUrl = "http://${HostName}:${Port}/actuator/health"
Write-Host "[+] Probing Edge API Gateway at: $healthUrl..." -NoNewline
try {
    $response = Invoke-RestMethod -Uri $healthUrl -Method Get -TimeoutSec 3 -ErrorAction Stop
    if ($response.status -eq "UP") {
        Write-Host " [ONLINE - UP]" -ForegroundColor Green
    } else {
        Write-Host " [STATUS: $($response.status)]" -ForegroundColor Yellow
    }
} catch {
    Write-Host " [UNREACHABLE]" -ForegroundColor Red
    Write-Warning "Could not connect to API Gateway at $healthUrl."
    Write-Warning "Make sure the banking microservices are running: docker compose --profile app up -d"
    $continue = Read-Host "Do you want to proceed anyway? (y/N)"
    if ($continue -ne "y" -and $continue -ne "Y") {
        exit 1
    }
}

# 4. Handle GUI Mode
if ($Gui) {
    Write-Host "[*] Launching JMeter GUI with test plan: $jmxPath" -ForegroundColor Cyan
    Start-Process -FilePath "jmeter" -ArgumentList "-t `"$jmxPath`""
    exit 0
}

# 5. Clean prior results
if ($Clean -and (Test-Path $resultsDir)) {
    Write-Host "[*] Cleaning previous results in $resultsDir..." -ForegroundColor DarkGray
    Remove-Item -Recurse -Force $resultsDir -ErrorAction SilentlyContinue
}

if (-not (Test-Path $resultsDir)) {
    New-Item -ItemType Directory -Path $resultsDir -Force | Out-Null
}

# 6. Execute Load Test in Non-GUI Mode
Write-Host "[*] Test Configuration:" -ForegroundColor White
Write-Host "    - Target Gateway   : http://${HostName}:${Port}" -ForegroundColor Gray
Write-Host "    - Virtual Users    : $Threads threads" -ForegroundColor Gray
Write-Host "    - Ramp-Up Duration : $RampUp seconds" -ForegroundColor Gray
Write-Host "    - Execution Time   : $Duration seconds" -ForegroundColor Gray
Write-Host "    - Transfer Amount  : $Amount PHP (Closed-Loop Ping-Pong)" -ForegroundColor Gray
Write-Host "    - SLA Assertion    : $SlaMs ms" -ForegroundColor Gray
Write-Host "    - Request Delay    : $DelayMs ms Gaussian pacing" -ForegroundColor Gray
Write-Host ""
Write-Host "[*] Starting Non-GUI JMeter execution..." -ForegroundColor Green

$jmeterArgs = @(
    "-n",
    "-t", "`"$jmxPath`"",
    "-l", "`"$jtlPath`"",
    "-e",
    "-o", "`"$reportDir`"",
    "-JHOST=$HostName",
    "-JPORT=$Port",
    "-JTHREADS=$Threads",
    "-JRAMP_UP=$RampUp",
    "-JDURATION=$Duration",
    "-JTRANSFER_AMOUNT=$Amount",
    "-JSLA_MS=$SlaMs",
    "-JDELAY_MS=$DelayMs"
)

$startTime = Get-Date
$process = Start-Process -FilePath "jmeter" -ArgumentList ($jmeterArgs -join " ") -NoNewWindow -PassThru -Wait
$elapsed = (Get-Date) - $startTime

Write-Host ""
if ($process.ExitCode -eq 0) {
    Write-Host "=================================================================" -ForegroundColor Green
    Write-Host "  Load Test Completed Successfully in $($elapsed.ToString('mm\:ss'))!           " -ForegroundColor Green
    Write-Host "=================================================================" -ForegroundColor Green
    Write-Host "Results JTL file : $jtlPath" -ForegroundColor Gray
    Write-Host "HTML Report      : $reportDir\index.html" -ForegroundColor Cyan

    if ($Report -or (Test-Path "$reportDir\index.html")) {
        if ($Report) {
            Write-Host "[*] Opening HTML Dashboard in browser..." -ForegroundColor Yellow
            Start-Process "$reportDir\index.html"
        } else {
            Write-Host "[TIP] To view the interactive dashboard, open:" -ForegroundColor Yellow
            Write-Host "      $reportDir\index.html" -ForegroundColor White
            Write-Host "      or run with the -Report switch: .\scripts\run_jmeter_test.ps1 -Report" -ForegroundColor DarkGray
        }
    }
} else {
    Write-Host "[-] JMeter exited with error code: $($process.ExitCode)" -ForegroundColor Red
    exit $process.ExitCode
}
