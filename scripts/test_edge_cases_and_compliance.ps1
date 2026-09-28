<#
.SYNOPSIS
    Automated Test Runner for CooBS Edge Cases, Compliance & Rejection Paths
.DESCRIPTION
    CLI equivalent of CooBS_Edge_Cases_And_Compliance.postman_collection.json.
    Tests:
    1. RBAC Guard (Customer accessing staff endpoints -> 403)
    2. Token Revocation (POST /token/revoke)
    3. Revoked Token Reuse Blocked by Redis (401)
    4. Transfer to Self Rejected (400)
    5. Missing Idempotency-Key Rejected (400)
    6. Non-Positive Amount Rejected (400)
    7. Non-Existent Account Rejected (404)
    8. Non-Zero Balance Account Closure Rejected (400)
    9. Account Freezing and Unfreezing (FROZEN -> ACTIVE)
    10. Imposing and Lifting Risk Holds (Flags)
    11. KYC Update Rejection Workflow
    12. Account Closure Rejection Workflow
    13. Notification Mark-as-Read State Transition
#>

[CmdletBinding()]
param(
    [string]$GatewayUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Continue"

Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host " CooBS - Edge Cases, Compliance & Invariant Guard Test Suite             " -ForegroundColor Cyan
Write-Host " Target Gateway: $GatewayUrl                                              " -ForegroundColor Cyan
Write-Host "==========================================================================" -ForegroundColor Cyan

$passed = 0
$total = 0

function Run-Step([string]$title, [scriptblock]$action) {
    $script:total++
    Write-Host "`n[$($script:total)] Testing: $title..." -ForegroundColor Yellow
    try {
        & $action
        Write-Host "  [PASS] $title" -ForegroundColor Green
        $script:passed++
    } catch {
        Write-Host "  [FAIL] $($title): $_" -ForegroundColor Red
    }
}

# 1. Customer Login
$custToken = $null
Run-Step "Customer Authentication (john_doe)" {
    $body = @{ username = "john_doe"; password = "Password123!" } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/auth/customers/login" -Method Post -ContentType "application/json" -Body $body
    $script:custToken = $res.authData.accessToken
    if (-not $script:custToken) { throw "No accessToken returned" }
}

# 2. RBAC Guard
Run-Step "RBAC Guard: Customer Accessing Staff Endpoint Rejected (403 Forbidden)" {
    try {
        Invoke-RestMethod -Uri "$GatewayUrl/api/v1/accounts" -Method Get -Headers @{ "Authorization" = "Bearer $script:custToken" }
        throw "Expected HTTP 403, but request succeeded!"
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        if ($status -ne 403) { throw "Expected 403, got $status" }
    }
}

# 3. Token Revocation & Blacklist Check
Run-Step "Token Revocation (POST /api/v1/auth/token/revoke)" {
    $revokeLogin = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/auth/customers/login" -Method Post -ContentType "application/json" -Body (@{ username = "david_kim"; password = "Password123!" } | ConvertTo-Json)
    $revokeToken = $revokeLogin.authData.accessToken
    
    $res = Invoke-WebRequest -Uri "$GatewayUrl/api/v1/auth/token/revoke" -Method Post -Headers @{ "Authorization" = "Bearer $revokeToken" } -UseBasicParsing -TimeoutSec 10
    if ($res.StatusCode -ne 200 -and $res.StatusCode -ne 204) { throw "Expected 204 or 200, got $($res.StatusCode)" }

    # Attempt reuse
    try {
        Invoke-RestMethod -Uri "$GatewayUrl/api/v1/auth/token/me" -Method Get -Headers @{ "Authorization" = "Bearer $revokeToken" }
        throw "Expected HTTP 401, but revoked token was accepted!"
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        if ($status -ne 401) { throw "Expected 401, got $status" }
    }
}

# 4. Transfer to Self
Run-Step "Ledger Invariant: Transfer to Self Rejected (400 Bad Request)" {
    try {
        $body = @{ sourceAccountId = 1; destinationAccountId = 1; amount = 50.0000; referenceNo = "REF-SELF"; currency = "PHP" } | ConvertTo-Json
        Invoke-RestMethod -Uri "$GatewayUrl/api/v1/ledger/transfer" -Method Post -Headers @{ "Authorization" = "Bearer $script:custToken"; "Idempotency-Key" = "self-test-key" } -ContentType "application/json" -Body $body
        throw "Expected HTTP 400, but request succeeded!"
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        if ($status -ne 400) { throw "Expected 400, got $status" }
    }
}

# 5. Missing Idempotency-Key
Run-Step "Ledger Invariant: Missing Idempotency-Key Rejected (400 Bad Request)" {
    try {
        $body = @{ sourceAccountId = 1; destinationAccountId = 2; amount = 50.0000; referenceNo = "REF-NO-IDEMP"; currency = "PHP" } | ConvertTo-Json
        Invoke-RestMethod -Uri "$GatewayUrl/api/v1/ledger/transfer" -Method Post -Headers @{ "Authorization" = "Bearer $script:custToken" } -ContentType "application/json" -Body $body
        throw "Expected HTTP 400, but request succeeded!"
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        if ($status -ne 400) { throw "Expected 400, got $status" }
    }
}

# 6. Non-Positive Amount
Run-Step "Ledger Invariant: Non-Positive Amount Rejected (400 Bad Request)" {
    try {
        $body = @{ sourceAccountId = 1; destinationAccountId = 2; amount = 0.0000; referenceNo = "REF-ZERO"; currency = "PHP" } | ConvertTo-Json
        Invoke-RestMethod -Uri "$GatewayUrl/api/v1/ledger/transfer" -Method Post -Headers @{ "Authorization" = "Bearer $script:custToken"; "Idempotency-Key" = "zero-amt-key" } -ContentType "application/json" -Body $body
        throw "Expected HTTP 400, but request succeeded!"
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        if ($status -ne 400) { throw "Expected 400, got $status" }
    }
}

# 7. Non-Existent Account
Run-Step "Ledger Invariant: Non-Existent Account Transfer Rejected (404 Not Found)" {
    try {
        $body = @{ sourceAccountId = 999999; destinationAccountId = 2; amount = 50.0000; referenceNo = "REF-NOT-FOUND"; currency = "PHP" } | ConvertTo-Json
        Invoke-RestMethod -Uri "$GatewayUrl/api/v1/ledger/transfer" -Method Post -Headers @{ "Authorization" = "Bearer $script:custToken"; "Idempotency-Key" = "not-found-key" } -ContentType "application/json" -Body $body
        throw "Expected HTTP 404, but request succeeded!"
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        if ($status -ne 404) { throw "Expected 404, got $status" }
    }
}

# 8. Non-Zero Balance Closure
Run-Step "Compliance Guard: Non-Zero Balance Account Closure Rejected (400 Bad Request)" {
    try {
        $body = @{ reason = "Attempt to close funded account" } | ConvertTo-Json
        Invoke-RestMethod -Uri "$GatewayUrl/api/v1/accounts/1/closure-request" -Method Post -Headers @{ "Authorization" = "Bearer $script:custToken" } -ContentType "application/json" -Body $body
        throw "Expected HTTP 400, but request succeeded!"
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        if ($status -ne 400) { throw "Expected 400, got $status" }
    }
}

# 9. Staff Login (Admin)
$adminToken = $null
Run-Step "Staff Authentication (admin)" {
    $body = @{ username = "admin"; password = "Password123!" } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/auth/staff/login" -Method Post -ContentType "application/json" -Body $body
    $script:adminToken = $res.authData.accessToken
    if (-not $script:adminToken) { throw "No adminToken returned" }
}

# 10. Account Freezing and Unfreezing
Run-Step "Account Status Transition: FREEZE and UNFREEZE" {
    # Freeze
    $freezeRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/accounts/2/status" -Method Patch -Headers @{ "Authorization" = "Bearer $script:adminToken" } -ContentType "application/json" -Body (@{ status = "FROZEN" } | ConvertTo-Json)
    if ($freezeRes.status -ne "FROZEN") { throw "Failed to freeze account" }
    
    # Unfreeze
    $unfreezeRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/accounts/2/status" -Method Patch -Headers @{ "Authorization" = "Bearer $script:adminToken" } -ContentType "application/json" -Body (@{ status = "ACTIVE" } | ConvertTo-Json)
    if ($unfreezeRes.status -ne "ACTIVE") { throw "Failed to unfreeze account" }
}

# 11. Imposing and Lifting Risk Holds (Flags)
Run-Step "Risk Hold Lifecycle: Add Flag and Remove Flag" {
    $flagRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/accounts/2/flags" -Method Post -Headers @{ "Authorization" = "Bearer $script:adminToken" } -ContentType "application/json" -Body (@{ reason = "Suspicious Transaction Audit" } | ConvertTo-Json)
    $flagId = $flagRes.flagId
    if (-not $flagId) { throw "No flagId returned" }

    $removeRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/accounts/2/flags/$flagId" -Method Delete -Headers @{ "Authorization" = "Bearer $script:adminToken" }
    if ($removeRes.status -ne "REMOVED") { throw "Failed to remove flag" }
}

# 12. KYC Rejection Workflow
Run-Step "Administrative Rejection: KYC Update Rejected (REJECTED)" {
    $kycReq = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/customers/kyc/update-request" -Method Post -Headers @{ "Authorization" = "Bearer $script:custToken" } -ContentType "application/json" -Body (@{ newFirstName = "John"; newLastName = "Doe-Invalid"; newAddress = "PO Box Only" } | ConvertTo-Json)
    $reqId = $kycReq.kycRequestId
    if (-not $reqId) { throw "No kycRequestId returned" }

    $rejectRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/customers/kyc/update-requests/$reqId/reject" -Method Post -Headers @{ "Authorization" = "Bearer $script:adminToken" } -ContentType "application/json" -Body (@{ rejectionReason = "PO Box addresses not allowed" } | ConvertTo-Json)
    if ($rejectRes.status -ne "REJECTED") { throw "Expected REJECTED status, got $($rejectRes.status)" }
}

# 13. Closure Rejection Workflow
Run-Step "Administrative Rejection: Account Closure Rejected (REJECTED)" {
    # Open fresh 0-balance account
    $accRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/accounts" -Method Post -Headers @{ "Authorization" = "Bearer $script:adminToken" } -ContentType "application/json" -Body (@{ customerId = 1; accountType = "SAVINGS"; currency = "PHP" } | ConvertTo-Json)
    $emptyAccId = $accRes.accountId

    # Request closure
    $closeReq = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/accounts/$emptyAccId/closure-request" -Method Post -Headers @{ "Authorization" = "Bearer $script:custToken" } -ContentType "application/json" -Body (@{ reason = "Closing empty account" } | ConvertTo-Json)
    $closeId = $closeReq.closureRequestId

    # Reject closure
    $rejectCloseRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/accounts/closure-requests/$closeId/reject" -Method Post -Headers @{ "Authorization" = "Bearer $script:adminToken" } -ContentType "application/json" -Body (@{ rejectionReason = "Branch hold pending" } | ConvertTo-Json)
    if ($rejectCloseRes.status -ne "REJECTED") { throw "Expected REJECTED status, got $($rejectCloseRes.status)" }
}

# 14. Notification Mark-as-Read State Transition
Run-Step "Notification State Transition (PATCH /read)" {
    $notifList = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/notifications/my-notifications?page=0&size=5" -Method Get -Headers @{ "Authorization" = "Bearer $script:custToken" }
    $items = if ($notifList.content) { $notifList.content } else { $notifList }
    if ($items.Count -gt 0) {
        $targetId = if ($items[0].notificationId) { $items[0].notificationId } else { $items[0].id }
        $markRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/notifications/$targetId/read" -Method Patch -Headers @{ "Authorization" = "Bearer $script:custToken" }
        if ($markRes.isRead -ne $true) { throw "Expected isRead: true" }
    }
}

Write-Host "`n==========================================================================" -ForegroundColor Cyan
Write-Host " Edge Cases & Compliance Summary: $passed / $total Tests Passed" -ForegroundColor Green
Write-Host "==========================================================================" -ForegroundColor Cyan
