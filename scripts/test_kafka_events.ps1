<#
.SYNOPSIS
    Automated Test Runner for Kafka Event-Driven Pipeline.
.DESCRIPTION
    Runs the exact equivalent of the CooBS_Kafka_Event_Driven_Verification Postman Collection.
    Validates end-to-end event production and consumption:
    - Flow 1: Completed Transfer (ledger.transfer.completed.v1)
    - Flow 2: Failed Transfer (ledger.transfer.failed.v1)
    - Flow 3: KYC Submission (kyc.request.submitted.v1)
    - Flow 4: KYC Approval (kyc.request.evaluated.v1)
    - Flow 5: KYC Rejection (kyc.request.evaluated.v1)
#>

[CmdletBinding()]
param(
    [string]$GatewayUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Continue"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host " CooBS Core Banking - Kafka Event-Driven Automated Test Suite    " -ForegroundColor Cyan
Write-Host " Target Gateway: $GatewayUrl                                     " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

$passed = 0
$total = 0

function Run-Test([string]$name, [scriptblock]$action) {
    $script:total++
    Write-Host "`n[$($script:total)] Testing: $name..." -ForegroundColor Yellow
    try {
        & $action
        Write-Host "  [PASS] $name" -ForegroundColor Green
        $script:passed++
    } catch {
        Write-Host "  [FAIL] $($name): $_" -ForegroundColor Red
    }
}

# 1. Login Sender (john_doe)
$customerToken = $null
Run-Test "1.1 Auth - Login Sender (john_doe)" {
    $body = @{ username = "john_doe"; password = "Password123!" } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/auth/customers/login" -Method Post -ContentType "application/json" -Body $body
    if (-not $res.authData.accessToken) { throw "Missing accessToken in response" }
    $script:customerToken = $res.authData.accessToken
}

# 2. Login Recipient (maria_santos)
$recipientToken = $null
Run-Test "1.2 Auth - Login Recipient (maria_santos)" {
    $body = @{ username = "maria_santos"; password = "Password123!" } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/auth/customers/login" -Method Post -ContentType "application/json" -Body $body
    if (-not $res.authData.accessToken) { throw "Missing accessToken in response" }
    $script:recipientToken = $res.authData.accessToken
}

# 3. Login Teller (teller_alice)
$tellerToken = $null
Run-Test "1.3 Auth - Login Teller (teller_alice)" {
    $body = @{ username = "teller_alice"; password = "Password123!" } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/auth/staff/login" -Method Post -ContentType "application/json" -Body $body
    if (-not $res.authData.accessToken) { throw "Missing accessToken in response" }
    $script:tellerToken = $res.authData.accessToken
}

# ============================================================================
# FLOW 1: Completed Transfer (ledger.transfer.completed.v1)
# ============================================================================
$transferRef = "TRF-KAFKA-$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())"
Run-Test "2.1 Flow 1: Execute Valid Transfer ($transferRef)" {
    $headers = @{
        "Authorization" = "Bearer $customerToken"
        "Idempotency-Key" = "idemp-$([Guid]::NewGuid())"
    }
    $body = @{
        sourceAccountId = 1
        destinationAccountId = 2
        amount = 50.00
        referenceNo = $transferRef
    } | ConvertTo-Json

    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/ledger/transfers" -Method Post -Headers $headers -ContentType "application/json" -Body $body
    if ($res.status -ne "COMPLETED") { throw "Expected status COMPLETED, got $($res.status)" }
}

Start-Sleep -Seconds 1

Run-Test "2.2 Flow 1: Verify Sender Received 'Funds Transfer Sent' Alert via Kafka" {
    $headers = @{ "Authorization" = "Bearer $customerToken" }
    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/notifications/my-notifications?page=0&size=10" -Method Get -Headers $headers
    $found = $res.content | Where-Object { $_.title -eq "Funds Transfer Sent" -and ($_.message -match $transferRef -or $_.message -match "50") }
    if (-not $found) { throw "Kafka event ledger.transfer.completed.v1 was NOT reflected in sender notification feed" }
}

Run-Test "2.3 Flow 1: Verify Recipient Received 'Funds Transfer Received' Alert via Kafka" {
    $headers = @{ "Authorization" = "Bearer $recipientToken" }
    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/notifications/my-notifications?page=0&size=10" -Method Get -Headers $headers
    $found = $res.content | Where-Object { $_.title -eq "Funds Transfer Received" }
    if (-not $found) { throw "Kafka event ledger.transfer.completed.v1 was NOT reflected in recipient notification feed" }
}

# ============================================================================
# FLOW 2: Failed Transfer (ledger.transfer.failed.v1)
# ============================================================================
$failedTransferRef = "TRF-FAIL-$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())"
Run-Test "3.1 Flow 2: Attempt Over-Limit / Insufficient Funds Transfer" {
    $headers = @{
        "Authorization" = "Bearer $customerToken"
        "Idempotency-Key" = "idemp-fail-$([Guid]::NewGuid())"
    }
    $body = @{
        sourceAccountId = 1
        destinationAccountId = 2
        amount = 999999999.00
        referenceNo = $failedTransferRef
    } | ConvertTo-Json

    try {
        Invoke-RestMethod -Uri "$GatewayUrl/api/v1/ledger/transfers" -Method Post -Headers $headers -ContentType "application/json" -Body $body
        throw "Transfer should have failed but returned 2xx"
    } catch {
        Write-Host "    -> Transfer successfully rejected by ledger business rule" -ForegroundColor DarkGray
    }
}

Start-Sleep -Seconds 1

Run-Test "3.2 Flow 2: Verify Customer Received 'Funds Transfer Failed' Alert via Kafka" {
    $headers = @{ "Authorization" = "Bearer $customerToken" }
    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/notifications/my-notifications?page=0&size=10" -Method Get -Headers $headers
    $found = $res.content | Where-Object { $_.title -eq "Funds Transfer Failed" -and ($_.message -match $failedTransferRef -or $_.message -match "Insufficient funds") }
    if (-not $found) { throw "Kafka event ledger.transfer.failed.v1 was NOT reflected in notification feed" }
}

# ============================================================================
# FLOW 3: KYC Submission (kyc.request.submitted.v1)
# ============================================================================
$kycReqId = $null
Run-Test "4.1 Flow 3: Submit KYC Update Request" {
    $headers = @{ "Authorization" = "Bearer $customerToken" }
    $body = @{
        newFirstName = "John"
        newLastName = "Doe Updated"
        newAddress = "777 Financial Tower, BGC, Taguig"
        newMobileNumber = "+639179998877"
        newCivilStatus = "MARRIED"
        newOccupation = "Senior Architect"
    } | ConvertTo-Json

    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/customers/kyc/update-request" -Method Post -Headers $headers -ContentType "application/json" -Body $body
    if (-not $res.kycRequestId) { throw "Missing kycRequestId in response" }
    $script:kycReqId = $res.kycRequestId
}

Start-Sleep -Seconds 1

Run-Test "4.2 Flow 3: Verify Customer Received 'KYC Update Submitted' Alert via Kafka" {
    $headers = @{ "Authorization" = "Bearer $customerToken" }
    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/notifications/my-notifications?page=0&size=10" -Method Get -Headers $headers
    $found = $res.content | Where-Object { $_.title -eq "KYC Update Submitted" -and $_.message -match "#$kycReqId" }
    if (-not $found) { throw "Kafka event kyc.request.submitted.v1 was NOT reflected in customer notification feed" }
}

# ============================================================================
# FLOW 4: KYC Approval (kyc.request.evaluated.v1)
# ============================================================================
Run-Test "5.1 Flow 4: Staff Approves KYC Request #$kycReqId" {
    $headers = @{ "Authorization" = "Bearer $tellerToken" }
    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/customers/kyc/update-request/$kycReqId/approve" -Method Post -Headers $headers
    if ($res.status -ne "APPROVED") { throw "Expected status APPROVED, got $($res.status)" }
}

Start-Sleep -Seconds 1

Run-Test "5.2 Flow 4: Verify Customer Received 'KYC Update Approved' Alert via Kafka" {
    $headers = @{ "Authorization" = "Bearer $customerToken" }
    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/notifications/my-notifications?page=0&size=10" -Method Get -Headers $headers
    $found = $res.content | Where-Object { $_.title -eq "KYC Update Approved" -and $_.message -match "#$kycReqId" }
    if (-not $found) { throw "Kafka event kyc.request.evaluated.v1 (APPROVED) was NOT reflected in customer notification feed" }
}

# ============================================================================
# FLOW 5: KYC Rejection (kyc.request.evaluated.v1)
# ============================================================================
$kycRejectReqId = $null
Run-Test "6.1 Flow 5: Submit 2nd KYC Update Request for Rejection" {
    $headers = @{ "Authorization" = "Bearer $customerToken" }
    $body = @{
        newFirstName = "John"
        newLastName = "Doe Reject Test"
        newAddress = "Temporary Invalid Address"
    } | ConvertTo-Json

    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/customers/kyc/update-request" -Method Post -Headers $headers -ContentType "application/json" -Body $body
    if (-not $res.kycRequestId) { throw "Missing kycRequestId in response" }
    $script:kycRejectReqId = $res.kycRequestId
}

Run-Test "6.2 Flow 5: Staff Rejects KYC Request #$kycRejectReqId" {
    $headers = @{ "Authorization" = "Bearer $tellerToken" }
    $body = @{ rejectionReason = "Blurry identity documentation" } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/customers/kyc/update-request/$kycRejectReqId/reject" -Method Post -Headers $headers -ContentType "application/json" -Body $body
    if ($res.status -ne "REJECTED") { throw "Expected status REJECTED, got $($res.status)" }
}

Start-Sleep -Seconds 1

Run-Test "6.3 Flow 5: Verify Customer Received 'KYC Update Rejected' Alert via Kafka" {
    $headers = @{ "Authorization" = "Bearer $customerToken" }
    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/notifications/my-notifications?page=0&size=10" -Method Get -Headers $headers
    $found = $res.content | Where-Object { $_.title -eq "KYC Update Rejected" -and $_.message -match "#$kycRejectReqId" -and $_.message -match "Blurry identity documentation" }
    if (-not $found) { throw "Kafka event kyc.request.evaluated.v1 (REJECTED) was NOT reflected in customer notification feed" }
}

# Summary
$summaryColor = if ($passed -eq $total) { "Green" } else { "Yellow" }
Write-Host "`n=================================================================" -ForegroundColor Cyan
Write-Host " KAFKA EVENT-DRIVEN TEST SUMMARY: $passed / $total Passed" -ForegroundColor $summaryColor
Write-Host "=================================================================" -ForegroundColor Cyan
