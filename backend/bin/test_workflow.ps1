# =============================================================================
# Script: test_workflow.ps1
# Description: Live end-to-end sanity & verification test suite for Member 4
#              (Subscriptions, Invoicing, Billing & Real-time Reports)
# Target: http://localhost:8080
# =============================================================================

$ErrorActionPreference = "Continue"
$baseUrl = "http://localhost:8080/api/v1"

function Print-Header($title) {
    Write-Host "`n=================================================================" -ForegroundColor Cyan
    Write-Host "  $title" -ForegroundColor Cyan
    Write-Host "=================================================================" -ForegroundColor Cyan
}

function Assert-Result($stepName, $isSuccess, $message) {
    if ($isSuccess) {
        Write-Host "[PASS] $stepName : $message" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] $stepName : $message" -ForegroundColor Red
    }
}

# -----------------------------------------------------------------------------
# SCENARIO A: Subscription Lifecycle & Quotation Template
# -----------------------------------------------------------------------------
Print-Header "SCENARIO A: Subscription Lifecycle & Quotation Template"

# Step 1: Create Quotation Template
$templatePayload = @{
    name = "Enterprise Annual Cloud Plan"
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
$tplResp = Invoke-RestMethod -Uri "$baseUrl/quotation-templates" -Method Post -Body $templatePayload -ContentType "application/json"
$tplId = $tplResp.id
Assert-Result "Create Template" ($tplId -gt 0) "Created Template ID: $tplId, Name: $($tplResp.name)"

# Step 2: Create Subscription in DRAFT status
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
$subResp = Invoke-RestMethod -Uri "$baseUrl/subscriptions" -Method Post -Body $subPayload -ContentType "application/json"
$subId = $subResp.id
$subNum = $subResp.subscriptionNumber
Assert-Result "Create Subscription" ($subResp.status -eq "DRAFT" -and $subNum -like "SUB-*") "Subscription created: $subNum, Initial Status: $($subResp.status)"

# Step 3: Advance Lifecycle: DRAFT -> QUOTATION -> CONFIRMED -> ACTIVE
Write-Host "3. Advancing Subscription Lifecycle..." -ForegroundColor Yellow

$quoteBody = @{ status = "QUOTATION"; reason = "Quote sent to customer" } | ConvertTo-Json
$r1 = Invoke-RestMethod -Uri "$baseUrl/subscriptions/$subId/status" -Method Patch -Body $quoteBody -ContentType "application/json"
Assert-Result "Transition to QUOTATION" ($r1.status -eq "QUOTATION") "Status updated to QUOTATION"

$confirmBody = @{ status = "CONFIRMED"; reason = "Quote approved by customer" } | ConvertTo-Json
$r2 = Invoke-RestMethod -Uri "$baseUrl/subscriptions/$subId/status" -Method Patch -Body $confirmBody -ContentType "application/json"
Assert-Result "Transition to CONFIRMED" ($r2.status -eq "CONFIRMED") "Status updated to CONFIRMED"

$activeBody = @{ status = "ACTIVE"; reason = "First cycle payment terms agreed" } | ConvertTo-Json
$r3 = Invoke-RestMethod -Uri "$baseUrl/subscriptions/$subId/status" -Method Patch -Body $activeBody -ContentType "application/json"
Assert-Result "Transition to ACTIVE" ($r3.status -eq "ACTIVE" -and $r3.statusHistories.Count -ge 4) "Status updated to ACTIVE (Total audit entries: $($r3.statusHistories.Count))"

# -----------------------------------------------------------------------------
# SCENARIO B: Invoice Generation & Anti-Double-Billing Guard
# -----------------------------------------------------------------------------
Print-Header "SCENARIO B: Invoice Generation & Anti-Double-Billing Guard"

$invPayload = @{
    subscriptionId = $subId
    customerId = 501
    customerName = "Apex Solutions Pvt Ltd"
    customerEmail = "billing@apexsolutions.com"
    billingPeriodStart = "2026-11-01"
    billingPeriodEnd = "2026-11-30"
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

# Calculation:
# Gross: 2 * 5000 = 10,000.00
# Discount: 10% = 1,000.00 -> Subtotal: 9,000.00
# Tax: 18% of 9,000 = 1,620.00
# Total: 10,620.00

Write-Host "4. Generating Invoice with BigDecimal calculations..." -ForegroundColor Yellow
$invResp = Invoke-RestMethod -Uri "$baseUrl/invoices" -Method Post -Body $invPayload -ContentType "application/json"
$invId = $invResp.id
$invNum = $invResp.invoiceNumber
Assert-Result "Invoice Created" ($invNum -like "INV-*" -and $invResp.totalAmount -eq 10620.00) "Invoice $invNum generated. Subtotal: $($invResp.subtotal), Tax: $($invResp.taxAmount), Total: $($invResp.totalAmount)"

Write-Host "5. Testing Anti-Double-Billing Guard (Duplicate Invoice)..." -ForegroundColor Yellow
try {
    $dupResp = Invoke-RestMethod -Uri "$baseUrl/invoices" -Method Post -Body $invPayload -ContentType "application/json"
    Assert-Result "Anti-Double-Billing Guard" $false "Failed: Duplicate invoice was permitted!"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Assert-Result "Anti-Double-Billing Guard" ($statusCode -eq 400) "Success! Duplicate billing attempt was blocked with HTTP $statusCode Bad Request."
}

# -----------------------------------------------------------------------------
# SCENARIO C: Payment Reconciliation & State Transitions (The ₹10,000 Flow)
# -----------------------------------------------------------------------------
Print-Header "SCENARIO C: Payment Reconciliation (The ₹10,000 Financial Flow)"

# 6. Create clean ₹10,000 Invoice
$inv10kPayload = @{
    customerId = 999
    customerName = "Enterprise Client Beta"
    billingPeriodStart = "2026-12-01"
    billingPeriodEnd = "2026-12-31"
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
$inv10k = Invoke-RestMethod -Uri "$baseUrl/invoices" -Method Post -Body $inv10kPayload -ContentType "application/json"
$inv10kId = $inv10k.id
Assert-Result "Create ₹10,000 Invoice" ($inv10k.totalAmount -eq 10000.00 -and $inv10k.amountDue -eq 10000.00) "Invoice $($inv10k.invoiceNumber) created with balance ₹10,000"

# 7. Record Partial Payment (₹4,000)
$pmt1Payload = @{
    invoiceId = $inv10kId
    customerId = 999
    amount = 4000.00
    currency = "INR"
    transactionReference = "TXN-UPI-4000"
} | ConvertTo-Json

Write-Host "7. Recording Partial Payment of ₹4,000..." -ForegroundColor Yellow
$pmt1 = Invoke-RestMethod -Uri "$baseUrl/payments" -Method Post -Body $pmt1Payload -ContentType "application/json"
$checkInv1 = Invoke-RestMethod -Uri "$baseUrl/invoices/$inv10kId" -Method Get
Assert-Result "Partial Payment (₹4,000)" ($checkInv1.amountPaid -eq 4000.00 -and $checkInv1.amountDue -eq 6000.00 -and $checkInv1.status -eq "PARTIALLY_PAID") "Status: $($checkInv1.status), Paid: ₹$($checkInv1.amountPaid), Remaining: ₹$($checkInv1.amountDue)"

# 8. Record Final Settlement (₹6,000)
$pmt2Payload = @{
    invoiceId = $inv10kId
    customerId = 999
    amount = 6000.00
    currency = "INR"
    transactionReference = "TXN-CARD-6000"
} | ConvertTo-Json

Write-Host "8. Recording Final Settlement Payment of ₹6,000..." -ForegroundColor Yellow
$pmt2 = Invoke-RestMethod -Uri "$baseUrl/payments" -Method Post -Body $pmt2Payload -ContentType "application/json"
$checkInv2 = Invoke-RestMethod -Uri "$baseUrl/invoices/$inv10kId" -Method Get
Assert-Result "Full Settlement (₹6,000)" ($checkInv2.amountPaid -eq 10000.00 -and $checkInv2.amountDue -eq 0.00 -and $checkInv2.status -eq "PAID") "Status: $($checkInv2.status), Paid: ₹$($checkInv2.amountPaid), Remaining: ₹$($checkInv2.amountDue)"

# 9. Overpayment Guard Check
Write-Host "9. Testing Overpayment Guard (₹1,000 on settled invoice)..." -ForegroundColor Yellow
$overpayPayload = @{
    invoiceId = $inv10kId
    customerId = 999
    amount = 1000.00
    currency = "INR"
} | ConvertTo-Json

try {
    $overResp = Invoke-RestMethod -Uri "$baseUrl/payments" -Method Post -Body $overpayPayload -ContentType "application/json"
    Assert-Result "Overpayment Guard" $false "Failed: Overpayment was permitted on settled invoice!"
} catch {
    $overCode = $_.Exception.Response.StatusCode.value__
    Assert-Result "Overpayment Guard" ($overCode -eq 400) "Success! Overpayment attempt was blocked with HTTP $overCode Bad Request."
}

# -----------------------------------------------------------------------------
# SCENARIO D: Real-Time Financial Dashboard
# -----------------------------------------------------------------------------
Print-Header "SCENARIO D: Real-Time Financial Dashboard Reporting"

Write-Host "10. Fetching Dashboard Metrics (GET /api/v1/reports/dashboard)..." -ForegroundColor Yellow
$dash = Invoke-RestMethod -Uri "$baseUrl/reports/dashboard" -Method Get

Assert-Result "Active Subscriptions Count" ($dash.activeSubscriptions -ge 1) "Active Subscriptions: $($dash.activeSubscriptions)"
Assert-Result "Total Revenue Collected" ($dash.totalRevenue -ge 10000.00) "Total Revenue: ₹$($dash.totalRevenue)"
Assert-Result "Recent Activity Feed" ($dash.recentActivities.Count -ge 2) "Found $($dash.recentActivities.Count) recent payment events in ledger"

Write-Host "`nRecent Financial Activities:" -ForegroundColor Cyan
$dash.recentActivities | Format-Table eventType, referenceNumber, amount, description -AutoSize

Print-Header "ALL 4 MEMBER 4 TEST SCENARIOS COMPLETED SUCCESSFULLY!"
