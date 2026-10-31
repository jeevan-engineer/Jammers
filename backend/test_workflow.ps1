# =============================================================================
# Script: test_workflow.ps1
# Description: Live end-to-end sanity & verification test suite for Member 4
#              (Subscriptions, Invoicing, Billing & Real-time Reports)
# Target: http://localhost:8080
# =============================================================================

$baseUrl = "http://localhost:8080/api/v1"

$global:passedCount = 0
$global:failedCount = 0
$global:skippedCount = 0

function Print-Header($title) {
    Write-Host "`n=================================================================" -ForegroundColor Cyan
    Write-Host "  $title" -ForegroundColor Cyan
    Write-Host "=================================================================" -ForegroundColor Cyan
}

function Assert-Result($stepName, $isSuccess, $message) {
    if ($isSuccess) {
        $global:passedCount++
        Write-Host "[PASS] $stepName : $message" -ForegroundColor Green
    } else {
        $global:failedCount++
        Write-Host "[FAIL] $stepName : $message" -ForegroundColor Red
    }
}

function Skip-Result($stepName, $reason) {
    $global:skippedCount++
    Write-Host "[SKIP] $stepName : $reason" -ForegroundColor Yellow
}

# -----------------------------------------------------------------------------
# Pre-Flight Connectivity Check
# -----------------------------------------------------------------------------
Print-Header "PRE-FLIGHT CHECK: Server Connectivity"
Write-Host "Pinging $baseUrl/reports/dashboard..." -ForegroundColor DarkGray

try {
    $preflight = Invoke-RestMethod -Uri "$baseUrl/reports/dashboard" -Method Get -TimeoutSec 5 -ErrorAction Stop
    Write-Host "[OK] Backend server is reachable on http://localhost:8080`n" -ForegroundColor Green
} catch {
    Write-Host "[FATAL ERROR] Cannot connect to http://localhost:8080" -ForegroundColor Red
    Write-Host "The Spring Boot backend is not running or not listening on port 8080." -ForegroundColor Red
    Write-Host "`nPlease start the backend first using:" -ForegroundColor Yellow
    Write-Host '  $env:DB_PASSWORD = "your_mysql_password"' -ForegroundColor Yellow
    Write-Host '  .\mvnw.cmd spring-boot:run' -ForegroundColor Yellow
    exit 1
}

# -----------------------------------------------------------------------------
# SCENARIO A: Subscription Lifecycle & Quotation Template
# -----------------------------------------------------------------------------
Print-Header "SCENARIO A: Subscription Lifecycle & Quotation Template"

$tplId = $null
$subId = $null

# Step 1: Create Quotation Template
$templatePayload = @{
    name = "Enterprise Annual Cloud Plan $(Get-Random)"
    description = "12-month pre-configured cloud template"
    recurringInterval = "MONTHLY"
    duration = 12
    durationUnit = "MONTHS"
    autoClose = $false
    active = $true
    lines = @(
        @{
            productId = 101
            productName = "Dedicated Cloud Instance"
            description = "High compute node"
            quantity = 1.00
            unitPrice = 5000.00
            discount = 0.00
            sequence = 1
        }
    )
} | ConvertTo-Json -Depth 5

Write-Host "1. Sending POST /api/v1/quotation-templates..." -ForegroundColor Yellow
try {
    $tplResp = Invoke-RestMethod -Uri "$baseUrl/quotation-templates" -Method Post -Body $templatePayload -ContentType "application/json" -ErrorAction Stop
    $tplId = $tplResp.id
    Assert-Result "Create Template" ($tplId -gt 0) "Created Template ID: $tplId, Name: $($tplResp.name)"
} catch {
    Assert-Result "Create Template" $false "Failed to create template: $($_.Exception.Message)"
}

# Step 2: Create Subscription in DRAFT status
if ($null -ne $tplId) {
    $subPayload = @{
        customerId = 501
        customerName = "Apex Solutions Pvt Ltd"
        customerEmail = "billing@apexsolutions.com"
        quotationTemplateId = $tplId
        billingInterval = "MONTHLY"
        intervalCount = 1
        startDate = "2026-11-01"
        currency = "INR"
        lines = @(
            @{
                productId = 101
                productName = "Dedicated Cloud Instance"
                quantity = 2.00
                unitPrice = 5000.00
                discount = 10.00
                taxRate = 18.00
            }
        )
    } | ConvertTo-Json -Depth 5

    Write-Host "2. Sending POST /api/v1/subscriptions..." -ForegroundColor Yellow
    try {
        $subResp = Invoke-RestMethod -Uri "$baseUrl/subscriptions" -Method Post -Body $subPayload -ContentType "application/json" -ErrorAction Stop
        $subId = $subResp.id
        $subNum = $subResp.subscriptionNumber
        Assert-Result "Create Subscription" ($subResp.status -eq "DRAFT" -and $subNum -like "SUB-*") "Subscription created: $subNum, Initial Status: $($subResp.status)"
    } catch {
        Assert-Result "Create Subscription" $false "Failed to create subscription: $($_.Exception.Message)"
    }
} else {
    Skip-Result "Create Subscription" "Skipped because template creation failed"
}

# Step 3: Advance Lifecycle: DRAFT -> QUOTATION -> CONFIRMED -> ACTIVE
if ($null -ne $subId) {
    Write-Host "3. Advancing Subscription Lifecycle..." -ForegroundColor Yellow
    try {
        $quoteBody = @{ status = "QUOTATION"; reason = "Quote sent to customer" } | ConvertTo-Json
        $r1 = Invoke-RestMethod -Uri "$baseUrl/subscriptions/$subId/status" -Method Patch -Body $quoteBody -ContentType "application/json" -ErrorAction Stop
        Assert-Result "Transition to QUOTATION" ($r1.status -eq "QUOTATION") "Status updated to QUOTATION"

        $confirmBody = @{ status = "CONFIRMED"; reason = "Quote approved by customer" } | ConvertTo-Json
        $r2 = Invoke-RestMethod -Uri "$baseUrl/subscriptions/$subId/status" -Method Patch -Body $confirmBody -ContentType "application/json" -ErrorAction Stop
        Assert-Result "Transition to CONFIRMED" ($r2.status -eq "CONFIRMED") "Status updated to CONFIRMED"

        $activeBody = @{ status = "ACTIVE"; reason = "First cycle payment terms agreed" } | ConvertTo-Json
        $r3 = Invoke-RestMethod -Uri "$baseUrl/subscriptions/$subId/status" -Method Patch -Body $activeBody -ContentType "application/json" -ErrorAction Stop
        Assert-Result "Transition to ACTIVE" ($r3.status -eq "ACTIVE" -and $r3.statusHistories.Count -ge 4) "Status updated to ACTIVE (Total audit entries: $($r3.statusHistories.Count))"
    } catch {
        Assert-Result "Advance Lifecycle" $false "Failed state transition: $($_.Exception.Message)"
    }
} else {
    Skip-Result "Advance Lifecycle" "Skipped because subscription creation failed"
}

# -----------------------------------------------------------------------------
# SCENARIO B: Invoice Generation & Anti-Double-Billing Guard
# -----------------------------------------------------------------------------
Print-Header "SCENARIO B: Invoice Generation & Anti-Double-Billing Guard"

$invId = $null

if ($null -ne $subId) {
    # Generate unique test billing period for this run
    $randomMonth = (Get-Random -Minimum 1 -Maximum 12).ToString("00")
    $pStart = "2027-$randomMonth-01"
    $pEnd = "2027-$randomMonth-28"

    $invPayload = @{
        subscriptionId = $subId
        customerId = 501
        customerName = "Apex Solutions Pvt Ltd"
        customerEmail = "billing@apexsolutions.com"
        billingPeriodStart = $pStart
        billingPeriodEnd = $pEnd
        status = "UNPAID"
        currency = "INR"
        lines = @(
            @{
                productId = 101
                productName = "Dedicated Cloud Instance"
                quantity = 2.00
                unitPrice = 5000.00
                discount = 10.00
                taxRate = 18.00
            }
        )
    } | ConvertTo-Json -Depth 5

    Write-Host "4. Generating Invoice with BigDecimal calculations..." -ForegroundColor Yellow
    try {
        $invResp = Invoke-RestMethod -Uri "$baseUrl/invoices" -Method Post -Body $invPayload -ContentType "application/json" -ErrorAction Stop
        $invId = $invResp.id
        $invNum = $invResp.invoiceNumber
        Assert-Result "Invoice Created" ($invNum -like "INV-*" -and [math]::Round($invResp.totalAmount, 2) -eq 10620.00) "Invoice $invNum generated. Subtotal: $($invResp.subtotal), Tax: $($invResp.taxAmount), Total: $($invResp.totalAmount)"
    } catch {
        Assert-Result "Invoice Created" $false "Failed to generate invoice: $($_.Exception.Message)"
    }

    Write-Host "5. Testing Anti-Double-Billing Guard (Duplicate Invoice)..." -ForegroundColor Yellow
    try {
        $dupResp = Invoke-RestMethod -Uri "$baseUrl/invoices" -Method Post -Body $invPayload -ContentType "application/json" -ErrorAction Stop
        Assert-Result "Anti-Double-Billing Guard" $false "Failed: Duplicate invoice was unexpectedly permitted!"
    } catch {
        $statusCode = $null
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }
        if ($statusCode -eq 400) {
            Assert-Result "Anti-Double-Billing Guard" $true "Success! Duplicate billing attempt was blocked with HTTP 400 Bad Request."
        } else {
            Assert-Result "Anti-Double-Billing Guard" $false "Unexpected status code $statusCode received: $($_.Exception.Message)"
        }
    }
} else {
    Skip-Result "Invoice Generation" "Skipped because subscription creation failed"
    Skip-Result "Anti-Double-Billing Guard" "Skipped because subscription creation failed"
}

# -----------------------------------------------------------------------------
# SCENARIO C: Payment Reconciliation & State Transitions (The ₹10,000 Flow)
# -----------------------------------------------------------------------------
Print-Header "SCENARIO C: Payment Reconciliation (The ₹10,000 Financial Flow)"

$inv10kId = $null

$inv10kPayload = @{
    customerId = 999
    customerName = "Enterprise Client Beta"
    billingPeriodStart = "2028-01-01"
    billingPeriodEnd = "2028-01-31"
    status = "UNPAID"
    currency = "INR"
    lines = @(
        @{
            productName = "Enterprise Annual License"
            quantity = 1.00
            unitPrice = 10000.00
            discount = 0.00
            taxRate = 0.00
        }
    )
} | ConvertTo-Json -Depth 5

Write-Host "6. Creating ₹10,000 Invoice..." -ForegroundColor Yellow
try {
    $inv10k = Invoke-RestMethod -Uri "$baseUrl/invoices" -Method Post -Body $inv10kPayload -ContentType "application/json" -ErrorAction Stop
    $inv10kId = $inv10k.id
    Assert-Result "Create ₹10,000 Invoice" ($inv10k.totalAmount -eq 10000.00 -and $inv10k.amountDue -eq 10000.00) "Invoice $($inv10k.invoiceNumber) created with balance ₹10,000"
} catch {
    Assert-Result "Create ₹10,000 Invoice" $false "Failed to create invoice: $($_.Exception.Message)"
}

if ($null -ne $inv10kId) {
    # 7. Record Partial Payment (₹4,000)
    $pmt1Payload = @{
        invoiceId = $inv10kId
        customerId = 999
        amount = 4000.00
        currency = "INR"
        transactionReference = "TXN-UPI-4000"
    } | ConvertTo-Json

    Write-Host "7. Recording Partial Payment of ₹4,000..." -ForegroundColor Yellow
    try {
        $pmt1 = Invoke-RestMethod -Uri "$baseUrl/payments" -Method Post -Body $pmt1Payload -ContentType "application/json" -ErrorAction Stop
        $checkInv1 = Invoke-RestMethod -Uri "$baseUrl/invoices/$inv10kId" -Method Get -ErrorAction Stop
        Assert-Result "Partial Payment (₹4,000)" ($checkInv1.amountPaid -eq 4000.00 -and $checkInv1.amountDue -eq 6000.00 -and $checkInv1.status -eq "PARTIALLY_PAID") "Status: $($checkInv1.status), Paid: ₹$($checkInv1.amountPaid), Remaining: ₹$($checkInv1.amountDue)"
    } catch {
        Assert-Result "Partial Payment (₹4,000)" $false "Failed partial payment: $($_.Exception.Message)"
    }

    # 8. Record Final Settlement (₹6,000)
    $pmt2Payload = @{
        invoiceId = $inv10kId
        customerId = 999
        amount = 6000.00
        currency = "INR"
        transactionReference = "TXN-CARD-6000"
    } | ConvertTo-Json

    Write-Host "8. Recording Final Settlement Payment of ₹6,000..." -ForegroundColor Yellow
    try {
        $pmt2 = Invoke-RestMethod -Uri "$baseUrl/payments" -Method Post -Body $pmt2Payload -ContentType "application/json" -ErrorAction Stop
        $checkInv2 = Invoke-RestMethod -Uri "$baseUrl/invoices/$inv10kId" -Method Get -ErrorAction Stop
        Assert-Result "Full Settlement (₹6,000)" ($checkInv2.amountPaid -eq 10000.00 -and $checkInv2.amountDue -eq 0.00 -and $checkInv2.status -eq "PAID") "Status: $($checkInv2.status), Paid: ₹$($checkInv2.amountPaid), Remaining: ₹$($checkInv2.amountDue)"
    } catch {
        Assert-Result "Full Settlement (₹6,000)" $false "Failed final payment: $($_.Exception.Message)"
    }

    # 9. Overpayment Guard Check
    Write-Host "9. Testing Overpayment Guard (₹1,000 on settled invoice)..." -ForegroundColor Yellow
    $overpayPayload = @{
        invoiceId = $inv10kId
        customerId = 999
        amount = 1000.00
        currency = "INR"
    } | ConvertTo-Json

    try {
        $overResp = Invoke-RestMethod -Uri "$baseUrl/payments" -Method Post -Body $overpayPayload -ContentType "application/json" -ErrorAction Stop
        Assert-Result "Overpayment Guard" $false "Failed: Overpayment was unexpectedly permitted on settled invoice!"
    } catch {
        $overCode = $null
        if ($_.Exception.Response) {
            $overCode = [int]$_.Exception.Response.StatusCode
        }
        if ($overCode -eq 400) {
            Assert-Result "Overpayment Guard" $true "Success! Overpayment attempt was blocked with HTTP 400 Bad Request."
        } else {
            Assert-Result "Overpayment Guard" $false "Unexpected status code $overCode received: $($_.Exception.Message)"
        }
    }
} else {
    Skip-Result "Partial Payment (₹4,000)" "Skipped because ₹10,000 invoice creation failed"
    Skip-Result "Full Settlement (₹6,000)" "Skipped because ₹10,000 invoice creation failed"
    Skip-Result "Overpayment Guard" "Skipped because ₹10,000 invoice creation failed"
}

# -----------------------------------------------------------------------------
# SCENARIO D: Real-Time Financial Dashboard
# -----------------------------------------------------------------------------
Print-Header "SCENARIO D: Real-Time Financial Dashboard Reporting"

Write-Host "10. Fetching Dashboard Metrics (GET /api/v1/reports/dashboard)..." -ForegroundColor Yellow
try {
    $dash = Invoke-RestMethod -Uri "$baseUrl/reports/dashboard" -Method Get -ErrorAction Stop
    Assert-Result "Active Subscriptions Count" ($dash.activeSubscriptions -ge 1) "Active Subscriptions: $($dash.activeSubscriptions)"
    Assert-Result "Total Revenue Collected" ($dash.totalRevenue -ge 10000.00) "Total Revenue: ₹$($dash.totalRevenue)"
    Assert-Result "Recent Activity Feed" ($dash.recentActivities.Count -ge 2) "Found $($dash.recentActivities.Count) recent payment events in ledger"

    Write-Host "`nRecent Financial Activities:" -ForegroundColor Cyan
    $dash.recentActivities | Format-Table eventType, referenceNumber, amount, description -AutoSize
} catch {
    Assert-Result "Dashboard Reporting" $false "Failed to fetch dashboard: $($_.Exception.Message)"
}

Print-Header "TEST SUMMARY"
Write-Host "Total Passed : $global:passedCount" -ForegroundColor Green
Write-Host "Total Failed : $global:failedCount" -ForegroundColor Red
Write-Host "Total Skipped: $global:skippedCount" -ForegroundColor Yellow

if ($global:failedCount -gt 0) {
    exit 1
} else {
    exit 0
}
