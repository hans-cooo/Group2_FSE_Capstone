<#
.SYNOPSIS
    Automated Runner for the End-to-End User Lifecycle Flow (CLI equivalent of Postman suite)
.DESCRIPTION
    Executes the 8-step lifecycle flow:
    1. Register new customer
    2. Customer Login
    3. Customer Submits KYC
    4. Teller Logs in & Opens Customer Account
    5. Customer Submits KYC Update Request
    6. Admin Logs in & Approves KYC Update
    7. Customer Submits Account Closure Request
    8. Admin Approves Account Closure
#>

[CmdletBinding()]
param(
    [string]$GatewayUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host " Core Retail Banking - User Lifecycle Flow Verification Suite            " -ForegroundColor Cyan
Write-Host " Target Gateway: $GatewayUrl                                              " -ForegroundColor Cyan
Write-Host "==========================================================================" -ForegroundColor Cyan

$passed = 0
$total = 8

# -----------------------------------------------------------------------------
# STEP 1: Register New Customer
# -----------------------------------------------------------------------------
Write-Host "`n[Step 1/8] Registering brand-new customer..." -ForegroundColor Yellow
$suffix = (Get-Date).Ticks % 1000000
$username = "flow_cust_$suffix"
$password = "Password123!"
$email = "flow_$suffix@bank.ph"

$regBody = @{
    username      = $username
    password      = $password
    email         = $email
    firstName     = "Alex"
    middleInitial = "M"
    lastName      = "Rivera"
    address       = "1204 Enterprise Center, Ayala Ave, Makati City"
    mobileNumber  = "+639175551234"
    civilStatus   = "Single"
    occupation    = "Software Architect"
} | ConvertTo-Json

$regRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/auth/customers/register" -Method Post -ContentType "application/json" -Body $regBody
$customerId = $regRes.userId
if (-not $customerId) { throw "Customer registration did not return a valid customerId" }
Write-Host "  [PASS] Registered customer '$username' with Customer ID: $customerId" -ForegroundColor Green
$passed++

# -----------------------------------------------------------------------------
# STEP 2: Customer Login
# -----------------------------------------------------------------------------
Write-Host "`n[Step 2/8] Customer logging in with credentials..." -ForegroundColor Yellow
$loginBody = @{ username = $username; password = $password } | ConvertTo-Json
$loginRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/auth/customers/login" -Method Post -ContentType "application/json" -Body $loginBody
$customerToken = $loginRes.authData.accessToken
if (-not $customerToken) { throw "Customer login did not return accessToken" }
Write-Host "  [PASS] Customer authenticated successfully. JWT Bearer token acquired." -ForegroundColor Green
$passed++

# -----------------------------------------------------------------------------
# STEP 3: Customer Submits Initial KYC
# -----------------------------------------------------------------------------
Write-Host "`n[Step 3/8] Customer submitting initial KYC details..." -ForegroundColor Yellow
$kycBody = @{
    firstName     = "Alex"
    middleInitial = "M"
    lastName      = "Rivera"
    address       = "1204 Enterprise Center, Ayala Ave, Makati City"
    civilStatus   = "Single"
    occupation    = "Software Architect"
    mobileNumber  = "+639175551234"
} | ConvertTo-Json

$kycHeaders = @{ "Authorization" = "Bearer $customerToken" }
$kycRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/customers/kyc/submit" -Method Post -Headers $kycHeaders -ContentType "application/json" -Body $kycBody
Write-Host "  [PASS] KYC submitted. Status: $($kycRes.status)" -ForegroundColor Green
$passed++

# -----------------------------------------------------------------------------
# STEP 4: Teller Logs in & Creates Customer Account
# -----------------------------------------------------------------------------
Write-Host "`n[Step 4/8] Teller logging in and opening account for customer..." -ForegroundColor Yellow
$tellerLogin = @{ username = "teller_alice"; password = "Password123!" } | ConvertTo-Json
$tellerRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/auth/staff/login" -Method Post -ContentType "application/json" -Body $tellerLogin
$tellerToken = $tellerRes.authData.accessToken

$accBody = @{
    customerId  = $customerId
    accountType = "SAVINGS"
    currency    = "PHP"
} | ConvertTo-Json

$accHeaders = @{ "Authorization" = "Bearer $tellerToken" }
$accRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/accounts" -Method Post -Headers $accHeaders -ContentType "application/json" -Body $accBody
$accountId = $accRes.accountId
$accountNumber = $accRes.accountNumber
if (-not $accountId) { throw "Account creation failed" }
Write-Host "  [PASS] Teller created account #$accountNumber (Account ID: $accountId, Type: $($accRes.accountType), Status: $($accRes.status))" -ForegroundColor Green
$passed++

# -----------------------------------------------------------------------------
# STEP 5: Customer Submits KYC Update Request
# -----------------------------------------------------------------------------
Write-Host "`n[Step 5/8] Customer submitting KYC update request..." -ForegroundColor Yellow
$updateKycBody = @{
    newFirstName     = "Alexander"
    newMiddleInitial = "M"
    newLastName      = "Rivera-Cruz"
    newAddress       = "Tower 2 High Street South, BGC, Taguig City"
    newMobileNumber  = "+639189991234"
    newCivilStatus   = "Married"
    newOccupation    = "Chief Technology Officer"
} | ConvertTo-Json

$updateKycRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/customers/kyc/update-request" -Method Post -Headers $kycHeaders -ContentType "application/json" -Body $updateKycBody
$kycRequestId = $updateKycRes.kycRequestId
if (-not $kycRequestId) { throw "KYC update request failed to return kycRequestId" }
Write-Host "  [PASS] KYC update request #$kycRequestId submitted with status: $($updateKycRes.status)" -ForegroundColor Green
$passed++

# -----------------------------------------------------------------------------
# STEP 6: Admin Logs in & Approves KYC Update
# -----------------------------------------------------------------------------
Write-Host "`n[Step 6/8] Admin logging in and approving KYC update request #$kycRequestId..." -ForegroundColor Yellow
$adminLogin = @{ username = "admin"; password = "Password123!" } | ConvertTo-Json
$adminRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/auth/staff/login" -Method Post -ContentType "application/json" -Body $adminLogin
$adminToken = $adminRes.authData.accessToken

$adminHeaders = @{ "Authorization" = "Bearer $adminToken" }
$approveKycRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/customers/kyc/update-requests/$kycRequestId/approve" -Method Post -Headers $adminHeaders
if ($approveKycRes.status -ne "APPROVED") { throw "Expected APPROVED status, got $($approveKycRes.status)" }
Write-Host "  [PASS] Admin approved KYC update #$kycRequestId. Profile updated to status: $($approveKycRes.status)" -ForegroundColor Green
$passed++

# -----------------------------------------------------------------------------
# STEP 7: Customer Submits Account Closure Request
# -----------------------------------------------------------------------------
Write-Host "`n[Step 7/8] Customer submitting closure request for account #$accountNumber..." -ForegroundColor Yellow
$closureBody = @{
    reason = "Relocating abroad and consolidating bank accounts"
} | ConvertTo-Json

$closureRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/accounts/$accountId/closure-request" -Method Post -Headers $kycHeaders -ContentType "application/json" -Body $closureBody
$closureRequestId = $closureRes.closureRequestId
if (-not $closureRequestId) { throw "Closure request failed to return closureRequestId" }
Write-Host "  [PASS] Closure request #$closureRequestId submitted with status: $($closureRes.status)" -ForegroundColor Green
$passed++

# -----------------------------------------------------------------------------
# STEP 8: Admin Approves Account Closure Request
# -----------------------------------------------------------------------------
Write-Host "`n[Step 8/8] Admin approving account closure request #$closureRequestId..." -ForegroundColor Yellow
$approveClosureRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/accounts/closure-requests/$closureRequestId/approve" -Method Post -Headers $adminHeaders
if ($approveClosureRes.accountStatus -ne "CLOSED") { throw "Expected accountStatus CLOSED, got $($approveClosureRes.accountStatus)" }
Write-Host "  [PASS] Admin approved closure request #$closureRequestId." -ForegroundColor Green
Write-Host "         Result: $($approveClosureRes.message)" -ForegroundColor Green
$passed++

# -----------------------------------------------------------------------------
# SUMMARY
# -----------------------------------------------------------------------------
Write-Host "`n==========================================================================" -ForegroundColor Cyan
Write-Host " User Lifecycle Flow Summary: $passed / $total Steps Passed" -ForegroundColor Green
Write-Host "==========================================================================" -ForegroundColor Cyan
