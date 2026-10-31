# =============================================================================
# Script: test_workflow.ps1
# Description: Live end-to-end sanity & verification test suite for Member 4
#              (Subscriptions, Billing, Invoicing, Payments & Reporting)
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

$runTag = (Get-Random -Minimum 1000 -Maximum 9999)

# -----------------------------------------------------------------------------
# SCENARIO A: Quotation Templates & Subscription Lifecycle
# -----------------------------------------------------------------------------
Print-Header "SCENARIO A: Quotation Templates & Subscription Lifecycle"

$tplId = $null
$subId = $null

# Step 1: Create Quotation Template
$templatePayload = @{
    name = "Cloud Platform Enterprise $runTag"
    description = "Pre-configured cloud subscription template"
    recurringInterval = "MONTHLY"
    duration = 12
    durationUnit = "MONTHS"
    validityDays = 45
    autoClose = $false
    active = $true
    lines = @(
        @{
            productId = 101
            productName = "Kubernetes Node Cluster"
            description = "High memory production cluster"
            quantity = 2.00
            unitPrice = 4500.00
            discount = 0.00
            taxRate = 18.00
            sequence = 1
        }
    )
} | ConvertTo-Json -Depth 5

Write-Host "1. Creating Quotation Template with tax and validity..." -ForegroundColor Yellow
try {
    $tplResp = Invoke-RestMethod -Uri "$baseUrl/quotation-templates" -Method Post -Body $templatePayload -ContentType "application/json" -ErrorAction Stop
    $tplId = $tplResp.id
    Assert-Result "Create Template" ($tplId -gt 0 -and $tplResp.validityDays -eq 45) "Template ID: $tplId, Name: $($tplResp.name), Validity: $($tplResp.validityDays) days"
} catch {
    Assert-Result "Create Template" $false "Failed to create template: $($_.Exception.Message)"
}

# Step 2: Generate Formal Quotation from Template
if ($null -ne $tplId) {
    $quoteGenPayload = @{
        customerId = 501
        customerName = "Apex Solutions Pvt Ltd"
        customerEmail = "billing@apexsolutions.com"
        startDate = (Get-Date).ToString("yyyy-MM-dd")
        validityDays = 30
        currency = "INR"
        notes = "Customized proposal based on Enterprise Plan"
    } | ConvertTo-Json

    Write-Host "2. Generating Quotation from Template (POST /quotation-templates/$tplId/generate-quotation)..." -ForegroundColor Yellow
    try {
        $quoteResp = Invoke-RestMethod -Uri "$baseUrl/quotation-templates/$tplId/generate-quotation" -Method Post -Body $quoteGenPayload -ContentType "application/json" -ErrorAction Stop
        $subId = $quoteResp.id
        $subNum = $quoteResp.subscriptionNumber
        Assert-Result "Generate Quotation" ($quoteResp.status -eq "QUOTATION" -and $subNum -like "SUB-*" -and $quoteResp.lines.Count -ge 1) "Generated Quote: $subNum, Status: $($quoteResp.status), Valid Until: $($quoteResp.quoteValidUntil)"
    } catch {
        Assert-Result "Generate Quotation" $false "Failed to generate quotation: $($_.Exception.Message)"
    }
} else {
    Skip-Result "Generate Quotation" "Skipped because template creation failed"
}

# Step 3: Advance Lifecycle: QUOTATION -> CONFIRMED -> ACTIVE
if ($null -ne $subId) {
    Write-Host "3. Advancing Subscription Lifecycle (QUOTATION -> CONFIRMED -> ACTIVE)..." -ForegroundColor Yellow
    try {
        $confirmBody = @{ status = "CONFIRMED"; reason = "Customer approved proposal signature" } | ConvertTo-Json
        $r1 = Invoke-RestMethod -Uri "$baseUrl/subscriptions/$subId/status" -Method Patch -Body $confirmBody -ContentType "application/json" -ErrorAction Stop
        Assert-Result "Transition to CONFIRMED" ($r1.status -eq "CONFIRMED") "Status updated to CONFIRMED"

        $activeBody = @{ status = "ACTIVE"; reason = "Contract execution complete" } | ConvertTo-Json
        $r2 = Invoke-RestMethod -Uri "$baseUrl/subscriptions/$subId/status" -Method Patch -Body $activeBody -ContentType "application/json" -ErrorAction Stop
        Assert-Result "Transition to ACTIVE" ($r2.status -eq "ACTIVE" -and $r2.statusHistories.Count -ge 3) "Status updated to ACTIVE (Audit trail count: $($r2.statusHistories.Count))"
    } catch {
        Assert-Result "Advance Lifecycle" $false "Failed state transition: $($_.Exception.Message)"
    }

    # Step 4: Pause Subscription
    Write-Host "4. Pausing Subscription (POST /subscriptions/$subId/pause)..." -ForegroundColor Yellow
    try {
        $pauseBody = @{ reason = "Temporary customer maintenance window" } | ConvertTo-Json
        $pauseResp = Invoke-RestMethod -Uri "$baseUrl/subscriptions/$subId/pause" -Method Post -Body $pauseBody -ContentType "application/json" -ErrorAction Stop
        Assert-Result "Pause Subscription" ($null -ne $pauseResp.pausedAt) "Subscription paused at: $($pauseResp.pausedAt)"
    } catch {
        Assert-Result "Pause Subscription" $false "Failed to pause subscription: $($_.Exception.Message)"
    }

    # Step 5: Resume Subscription
    Write-Host "5. Resuming Subscription (POST /subscriptions/$subId/resume)..." -ForegroundColor Yellow
    try {
        $resumeResp = Invoke-RestMethod -Uri "$baseUrl/subscriptions/$subId/resume" -Method Post -ErrorAction Stop
        Assert-Result "Resume Subscription" ($null -eq $resumeResp.pausedAt -and $resumeResp.status -eq "ACTIVE") "Subscription resumed successfully"
    } catch {
        Assert-Result "Resume Subscription" $false "Failed to resume subscription: $($_.Exception.Message)"
    }

    # Step 6: Renew Subscription
    Write-Host "6. Renewing Subscription (POST /subscriptions/$subId/renew)..." -ForegroundColor Yellow
    try {
        $renewBody = @{
            duration = 6
            durationUnit = "MONTHS"
            autoRenew = $true
            notes = "Contract renewed for additional 6 months"
        } | ConvertTo-Json
        $renewResp = Invoke-RestMethod -Uri "$baseUrl/subscriptions/$subId/renew" -Method Post -Body $renewBody -ContentType "application/json" -ErrorAction Stop
        Assert-Result "Renew Subscription" ($null -ne $renewResp.endDate) "Subscription renewed through: $($renewResp.endDate)"
    } catch {
        Assert-Result "Renew Subscription" $false "Failed to renew subscription: $($_.Exception.Message)"
    }

    # Step 7: Early Closure
    Write-Host "7. Closing Subscription Early (POST /subscriptions/$subId/close)..." -ForegroundColor Yellow
    try {
        $closeBody = @{ reason = "Customer business consolidation" } | ConvertTo-Json
        $closeResp = Invoke-RestMethod -Uri "$baseUrl/subscriptions/$subId/close" -Method Post -Body $closeBody -ContentType "application/json" -ErrorAction Stop
        Assert-Result "Early Closure" ($closeResp.status -eq "CLOSED") "Subscription status transitioned to CLOSED"
    } catch {
        Assert-Result "Early Closure" $false "Failed to close subscription: $($_.Exception.Message)"
    }
} else {
    Skip-Result "Advance Lifecycle" "Skipped"
    Skip-Result "Pause Subscription" "Skipped"
    Skip-Result "Resume Subscription" "Skipped"
    Skip-Result "Renew Subscription" "Skipped"
    Skip-Result "Early Closure" "Skipped"
}

# -----------------------------------------------------------------------------
# SCENARIO B: Invoice Generation, Lifecycle & Anti-Double-Billing
# -----------------------------------------------------------------------------
Print-Header "SCENARIO B: Invoice Generation, Lifecycle & Anti-Double-Billing"

# Step 8: Create an Active Subscription for Billing
$activeSubId = $null
$subActivePayload = @{
    customerId = 601
    customerName = "Matrix Dynamics Ltd"
    customerEmail = "accounts@matrix.com"
    billingInterval = "MONTHLY"
    intervalCount = 1
    startDate = "2027-01-01"
    status = "ACTIVE"
    currency = "INR"
    lines = @(
        @{
            productId = 201
            productName = "Managed PostgreSQL Database"
            quantity = 1.00
            unitPrice = 8000.00
            discount = 10.00
            taxRate = 18.00
        }
    )
} | ConvertTo-Json -Depth 5

Write-Host "8. Creating ACTIVE subscription for billing tests..." -ForegroundColor Yellow
try {
    $actSubResp = Invoke-RestMethod -Uri "$baseUrl/subscriptions" -Method Post -Body $subActivePayload -ContentType "application/json" -ErrorAction Stop
    $activeSubId = $actSubResp.id
    Assert-Result "Create Active Subscription" ($activeSubId -gt 0) "Created Subscription ID: $activeSubId ($($actSubResp.subscriptionNumber))"
} catch {
    Assert-Result "Create Active Subscription" $false "Failed to create subscription: $($_.Exception.Message)"
}

$generatedInvId = $null
if ($null -ne $activeSubId) {
    # Step 9: Single Invoice Generation
    Write-Host "9. Generating Single Invoice (POST /subscriptions/$activeSubId/generate-invoice)..." -ForegroundColor Yellow
    try {
        $singleInvResp = Invoke-RestMethod -Uri "$baseUrl/subscriptions/$activeSubId/generate-invoice" -Method Post -ErrorAction Stop
        $generatedInvId = $singleInvResp.invoiceId
        Assert-Result "Single Invoice Generated" ($singleInvResp.success -and $generatedInvId -gt 0) "Generated Invoice ID: $generatedInvId"
    } catch {
        Assert-Result "Single Invoice Generated" $false "Failed to generate single invoice: $($_.Exception.Message)"
    }

    # Step 10: Duplicate Billing Prevention Guard (Single Invoice endpoint)
    Write-Host "10. Testing Duplicate Billing Prevention on Single Invoice endpoint..." -ForegroundColor Yellow
    try {
        $dupResp = Invoke-RestMethod -Uri "$baseUrl/subscriptions/$activeSubId/generate-invoice" -Method Post -ErrorAction Stop
        Assert-Result "Single Invoice Duplicate Guard" $false "Failed: Duplicate invoice was unexpectedly permitted!"
    } catch {
        $sc = [int]$_.Exception.Response.StatusCode
        if ($sc -eq 400) {
            Assert-Result "Single Invoice Duplicate Guard" $true "Success! Duplicate billing blocked with HTTP 400 Bad Request."
        } else {
            Assert-Result "Single Invoice Duplicate Guard" $false "Unexpected HTTP status: $sc"
        }
    }

    # Step 11: Database Unique Constraint verification (uq_invoice_subscription_period)
    Write-Host "11. Testing Database Unique Constraint (uq_invoice_subscription_period)..." -ForegroundColor Yellow
    $dbDupPayload = @{
        subscriptionId = $activeSubId
        customerId = 601
        customerName = "Matrix Dynamics Ltd"
        billingPeriodStart = "2027-01-01"
        billingPeriodEnd = "2027-02-01"
        status = "UNPAID"
        currency = "INR"
        lines = @(
            @{
                productName = "Managed PostgreSQL Database"
                quantity = 1.00
                unitPrice = 8000.00
            }
        )
    } | ConvertTo-Json -Depth 5

    try {
        $dupDb = Invoke-RestMethod -Uri "$baseUrl/invoices" -Method Post -Body $dbDupPayload -ContentType "application/json" -ErrorAction Stop
        Assert-Result "Database Unique Billing Guard" $false "Failed: Duplicate period allowed by database constraint!"
    } catch {
        $sc = [int]$_.Exception.Response.StatusCode
        if ($sc -eq 400) {
            Assert-Result "Database Unique Billing Guard" $true "Success! Database duplicate constraint enforced with HTTP 400."
        } else {
            Assert-Result "Database Unique Billing Guard" $false "Unexpected status: $sc"
        }
    }
} else {
    Skip-Result "Single Invoice Generated" "Skipped"
    Skip-Result "Single Invoice Duplicate Guard" "Skipped"
    Skip-Result "Database Unique Billing Guard" "Skipped"
}

# Step 12: Batch Invoice Generation for Due Subscriptions
Write-Host "12. Testing Batch Invoice Billing with Eligible Due Subscription..." -ForegroundColor Yellow
$dueSubPayload = @{
    customerId = 605
    customerName = "Batch Verified Corp"
    customerEmail = "batch@corp.com"
    billingInterval = "MONTHLY"
    intervalCount = 1
    startDate = (Get-Date).AddMonths(-1).ToString("yyyy-MM-dd")
    status = "ACTIVE"
    currency = "INR"
    lines = @(
        @{
            productId = 301
            productName = "Elastic Storage Volume"
            quantity = 2.00
            unitPrice = 3000.00
            discount = 0.00
            taxRate = 18.00
        }
    )
} | ConvertTo-Json -Depth 5

try {
    $dueSub = Invoke-RestMethod -Uri "$baseUrl/subscriptions" -Method Post -Body $dueSubPayload -ContentType "application/json" -ErrorAction Stop
    $dueSubId = $dueSub.id

    # Execute Batch Billing
    $batchResp1 = Invoke-RestMethod -Uri "$baseUrl/subscriptions/generate-billing" -Method Post -ErrorAction Stop
    Assert-Result "Batch Billing Generated Invoices" ($batchResp1.success -and $batchResp1.invoicesGeneratedCount -ge 1) "Batch billing generated $($batchResp1.invoicesGeneratedCount) invoice(s)"

    # Verify created invoice
    $dueInvoices = Invoke-RestMethod -Uri "$baseUrl/invoices/subscription/$dueSubId" -Method Get -ErrorAction Stop
    Assert-Result "Batch Invoice Created For Due Subscription" ($dueInvoices.Count -eq 1 -and $dueInvoices[0].totalAmount -eq 7080.00) "Invoice $($dueInvoices[0].invoiceNumber) generated for due subscription (Total: ₹$($dueInvoices[0].totalAmount))"

    # Verify nextBillingDate has advanced
    $refreshedSub = Invoke-RestMethod -Uri "$baseUrl/subscriptions/$dueSubId" -Method Get -ErrorAction Stop
    $nextDate = [DateTime]::Parse($refreshedSub.nextBillingDate)
    Assert-Result "Batch Billing Advanced Next Date" ($nextDate -gt (Get-Date)) "Subscription nextBillingDate advanced to: $($refreshedSub.nextBillingDate)"

    # Execute Batch Billing again to verify duplicate billing protection
    $batchResp2 = Invoke-RestMethod -Uri "$baseUrl/subscriptions/generate-billing" -Method Post -ErrorAction Stop
    $dueInvoicesAfter = Invoke-RestMethod -Uri "$baseUrl/invoices/subscription/$dueSubId" -Method Get -ErrorAction Stop
    Assert-Result "Batch Billing Idempotency Guard" ($dueInvoicesAfter.Count -eq 1) "Confirmed no duplicate invoice created upon second batch run (Count: $($dueInvoicesAfter.Count))"
} catch {
    Assert-Result "Batch Billing Execution" $false "Failed batch billing: $($_.Exception.Message)"
}

# Step 13: Printable ASCII Invoice
if ($null -ne $generatedInvId) {
    Write-Host "13. Fetching Printable Invoice (GET /invoices/$generatedInvId/print)..." -ForegroundColor Yellow
    try {
        $printResp = Invoke-RestMethod -Uri "$baseUrl/invoices/$generatedInvId/print" -Method Get -ErrorAction Stop
        Assert-Result "Printable Invoice" ("$printResp" -match "BRIGHTLOOP INVOICE" -and "$printResp" -match "TOTAL AMOUNT") "Printable ASCII invoice formatted successfully"
    } catch {
        Assert-Result "Printable Invoice" $false "Failed to fetch printable invoice: $($_.Exception.Message)"
    }

    # Step 14: Send Invoice via Notification Boundary
    Write-Host "14. Dispatching Invoice Notification (POST /invoices/$generatedInvId/send)..." -ForegroundColor Yellow
    try {
        $sendResp = Invoke-RestMethod -Uri "$baseUrl/invoices/$generatedInvId/send" -Method Post -ErrorAction Stop
        Assert-Result "Send Invoice Notification" ($sendResp.success -eq $true) "Invoice dispatched via notification service boundary"
    } catch {
        Assert-Result "Send Invoice Notification" $false "Failed to send invoice: $($_.Exception.Message)"
    }
} else {
    Skip-Result "Printable Invoice" "Skipped"
    Skip-Result "Send Invoice Notification" "Skipped"
}

# Step 15: Create DRAFT invoice and Cancel it
$draftInvId = $null
$draftPayload = @{
    customerId = 701
    customerName = "Cancellation Test Org"
    billingPeriodStart = "2029-01-01"
    billingPeriodEnd = "2029-01-31"
    status = "DRAFT"
    currency = "INR"
    lines = @(
        @{
            productName = "One-time Setup Fee"
            quantity = 1.00
            unitPrice = 2500.00
        }
    )
} | ConvertTo-Json -Depth 5

Write-Host "15. Testing Invoice Lifecycle (DRAFT -> CANCELLED)..." -ForegroundColor Yellow
try {
    $draftResp = Invoke-RestMethod -Uri "$baseUrl/invoices" -Method Post -Body $draftPayload -ContentType "application/json" -ErrorAction Stop
    $draftInvId = $draftResp.id

    $cancelBody = @{ status = "CANCELLED" } | ConvertTo-Json
    $cancelResp = Invoke-RestMethod -Uri "$baseUrl/invoices/$draftInvId/status" -Method Patch -Body $cancelBody -ContentType "application/json" -ErrorAction Stop
    Assert-Result "Cancel Invoice" ($cancelResp.status -eq "CANCELLED") "Invoice $($cancelResp.invoiceNumber) successfully CANCELLED"
} catch {
    Assert-Result "Cancel Invoice" $false "Failed invoice cancellation: $($_.Exception.Message)"
}

# -----------------------------------------------------------------------------
# SCENARIO C: Payment Ledger & Financial Reconciliation (₹10,000 Flow)
# -----------------------------------------------------------------------------
Print-Header "SCENARIO C: Payment Processing & Financial Reconciliation (₹10,000 Flow)"

$inv10kId = $null
$inv10kPayload = @{
    customerId = 999
    customerName = "Enterprise Global Client"
    billingPeriodStart = "2028-05-01"
    billingPeriodEnd = "2028-05-31"
    status = "UNPAID"
    currency = "INR"
    lines = @(
        @{
            productName = "Dedicated Compute Cluster"
            quantity = 1.00
            unitPrice = 10000.00
            discount = 0.00
            taxRate = 0.00
        }
    )
} | ConvertTo-Json -Depth 5

Write-Host "16. Creating ₹10,000 Invoice for Payment Reconciliation..." -ForegroundColor Yellow
try {
    $inv10k = Invoke-RestMethod -Uri "$baseUrl/invoices" -Method Post -Body $inv10kPayload -ContentType "application/json" -ErrorAction Stop
    $inv10kId = $inv10k.id
    Assert-Result "Create ₹10,000 Invoice" ($inv10k.totalAmount -eq 10000.00 -and $inv10k.amountDue -eq 10000.00) "Invoice $($inv10k.invoiceNumber) created with amount ₹10,000"
} catch {
    Assert-Result "Create ₹10,000 Invoice" $false "Failed to create invoice: $($_.Exception.Message)"
}

if ($null -ne $inv10kId) {
    # Step 17: Partial Payment of ₹4,000
    $txn1 = "TXN-UPI-4000-$runTag"
    $pmt1Payload = @{
        invoiceId = $inv10kId
        customerId = 999
        amount = 4000.00
        paymentMethodType = "UPI"
        currency = "INR"
        transactionReference = $txn1
        notes = "First installment"
    } | ConvertTo-Json

    Write-Host "17. Recording Partial Payment of ₹4,000 via UPI..." -ForegroundColor Yellow
    try {
        $pmt1 = Invoke-RestMethod -Uri "$baseUrl/payments" -Method Post -Body $pmt1Payload -ContentType "application/json" -ErrorAction Stop
        $check1 = Invoke-RestMethod -Uri "$baseUrl/invoices/$inv10kId" -Method Get -ErrorAction Stop
        Assert-Result "Partial Payment (₹4,000)" ($check1.amountPaid -eq 4000.00 -and $check1.amountDue -eq 6000.00 -and $check1.status -eq "PARTIALLY_PAID") "Status: $($check1.status), Paid: ₹$($check1.amountPaid), Remaining: ₹$($check1.amountDue)"
    } catch {
        Assert-Result "Partial Payment (₹4,000)" $false "Failed partial payment: $($_.Exception.Message)"
    }

    # Step 18: Full Settlement Payment of ₹6,000
    $txn2 = "TXN-CARD-6000-$runTag"
    $pmt2Payload = @{
        invoiceId = $inv10kId
        customerId = 999
        amount = 6000.00
        paymentMethodType = "CREDIT_CARD"
        currency = "INR"
        transactionReference = $txn2
        notes = "Final settlement installment"
    } | ConvertTo-Json

    Write-Host "18. Recording Final Settlement Payment of ₹6,000 via CREDIT_CARD..." -ForegroundColor Yellow
    try {
        $pmt2 = Invoke-RestMethod -Uri "$baseUrl/payments" -Method Post -Body $pmt2Payload -ContentType "application/json" -ErrorAction Stop
        $check2 = Invoke-RestMethod -Uri "$baseUrl/invoices/$inv10kId" -Method Get -ErrorAction Stop
        Assert-Result "Full Settlement (₹6,000)" ($check2.amountPaid -eq 10000.00 -and $check2.amountDue -eq 0.00 -and $check2.status -eq "PAID") "Status: $($check2.status), Paid: ₹$($check2.amountPaid), Balance: ₹$($check2.amountDue)"
    } catch {
        Assert-Result "Full Settlement (₹6,000)" $false "Failed full settlement: $($_.Exception.Message)"
    }

    # Step 19: Overpayment Guard Check
    Write-Host "19. Testing Overpayment Guard (₹500 on fully paid invoice)..." -ForegroundColor Yellow
    $overPayload = @{
        invoiceId = $inv10kId
        customerId = 999
        amount = 500.00
        paymentMethodType = "CASH"
        currency = "INR"
        transactionReference = "TXN-OVER-500-$runTag"
    } | ConvertTo-Json

    try {
        $overResp = Invoke-RestMethod -Uri "$baseUrl/payments" -Method Post -Body $overPayload -ContentType "application/json" -ErrorAction Stop
        Assert-Result "Overpayment Guard" $false "Failed: Overpayment was unexpectedly permitted on settled invoice!"
    } catch {
        $sc = [int]$_.Exception.Response.StatusCode
        if ($sc -eq 400) {
            Assert-Result "Overpayment Guard" $true "Success! Overpayment attempt was blocked with HTTP 400 Bad Request."
        } else {
            Assert-Result "Overpayment Guard" $false "Unexpected status code: $sc"
        }
    }

    # Step 20: Duplicate Transaction Reference Guard
    Write-Host "20. Testing Duplicate Payment Reference Guard..." -ForegroundColor Yellow
    $dupTxnPayload = @{
        invoiceId = $inv10kId
        customerId = 999
        amount = 100.00
        paymentMethodType = "UPI"
        transactionReference = $txn1
    } | ConvertTo-Json

    try {
        $dupTxnResp = Invoke-RestMethod -Uri "$baseUrl/payments" -Method Post -Body $dupTxnPayload -ContentType "application/json" -ErrorAction Stop
        Assert-Result "Duplicate Txn Ref Guard" $false "Failed: Duplicate transaction reference permitted!"
    } catch {
        $sc = [int]$_.Exception.Response.StatusCode
        if ($sc -eq 400) {
            Assert-Result "Duplicate Txn Ref Guard" $true "Success! Duplicate reference blocked with HTTP 400 Bad Request."
        } else {
            Assert-Result "Duplicate Txn Ref Guard" $false "Unexpected status code: $sc"
        }
    }
} else {
    Skip-Result "Partial Payment (₹4,000)" "Skipped"
    Skip-Result "Full Settlement (₹6,000)" "Skipped"
    Skip-Result "Overpayment Guard" "Skipped"
    Skip-Result "Duplicate Txn Ref Guard" "Skipped"
}

# Step 21: Zero / Negative Payment Guard
Write-Host "21. Testing Zero & Negative Payment Guard..." -ForegroundColor Yellow
$negPayload = @{
    invoiceId = 1
    customerId = 999
    amount = -100.00
    paymentMethodType = "CASH"
} | ConvertTo-Json

try {
    $negResp = Invoke-RestMethod -Uri "$baseUrl/payments" -Method Post -Body $negPayload -ContentType "application/json" -ErrorAction Stop
    Assert-Result "Negative Payment Guard" $false "Failed: Negative payment permitted!"
} catch {
    $sc = [int]$_.Exception.Response.StatusCode
    if ($sc -eq 400) {
        Assert-Result "Negative Payment Guard" $true "Success! Negative payment rejected with HTTP 400 Bad Request."
    } else {
        Assert-Result "Negative Payment Guard" $false "Unexpected status code: $sc"
    }
}

# Step 22: Payment Against Cancelled Invoice Guard
if ($null -ne $draftInvId) {
    Write-Host "22. Testing Payment Against Cancelled Invoice Guard..." -ForegroundColor Yellow
    $cancelPmtPayload = @{
        invoiceId = $draftInvId
        customerId = 701
        amount = 1000.00
        paymentMethodType = "BANK_TRANSFER"
        transactionReference = "TXN-CANCEL-$runTag"
    } | ConvertTo-Json

    try {
        $cpResp = Invoke-RestMethod -Uri "$baseUrl/payments" -Method Post -Body $cancelPmtPayload -ContentType "application/json" -ErrorAction Stop
        Assert-Result "Payment on Cancelled Invoice Guard" $false "Failed: Payment permitted on cancelled invoice!"
    } catch {
        $sc = [int]$_.Exception.Response.StatusCode
        if ($sc -eq 400) {
            Assert-Result "Payment on Cancelled Invoice Guard" $true "Success! Payment on cancelled invoice blocked with HTTP 400."
        } else {
            Assert-Result "Payment on Cancelled Invoice Guard" $false "Unexpected status code: $sc"
        }
    }
} else {
    Skip-Result "Payment on Cancelled Invoice Guard" "Skipped"
}

# -----------------------------------------------------------------------------
# SCENARIO D: Financial Dashboard & Reporting
# -----------------------------------------------------------------------------
Print-Header "SCENARIO D: Real-Time Financial Dashboard & Reporting"

# Step 23: Dashboard Aggregates
Write-Host "23. Fetching Financial Dashboard Overview (GET /reports/dashboard)..." -ForegroundColor Yellow
try {
    $dash = Invoke-RestMethod -Uri "$baseUrl/reports/dashboard" -Method Get -ErrorAction Stop
    Assert-Result "Active Subscriptions Count" ($dash.activeSubscriptions -ge 1) "Active Subscriptions: $($dash.activeSubscriptions)"
    Assert-Result "Total Revenue Collected" ($dash.totalRevenue -ge 10000.00) "Total Revenue Collected: ₹$($dash.totalRevenue)"
    Assert-Result "Invoice Status Breakdown" ($dash.paidInvoices -ge 1 -and $null -ne $dash.partiallyPaidInvoices) "Paid Invoices: $($dash.paidInvoices), Partially Paid: $($dash.partiallyPaidInvoices), Unpaid: $($dash.unpaidInvoices)"
} catch {
    Assert-Result "Dashboard Overview" $false "Failed to fetch dashboard: $($_.Exception.Message)"
}

# Step 24: Dashboard with Date Range Filter
Write-Host "24. Fetching Dashboard with Date Filter (?startDate=2020-01-01&endDate=2030-12-31)..." -ForegroundColor Yellow
try {
    $filteredDash = Invoke-RestMethod -Uri "$baseUrl/reports/dashboard?startDate=2020-01-01&endDate=2030-12-31" -Method Get -ErrorAction Stop
    Assert-Result "Date Filtered Dashboard" ($filteredDash.totalRevenue -ge 10000.00) "Filtered Dashboard Revenue: ₹$($filteredDash.totalRevenue)"
} catch {
    Assert-Result "Date Filtered Dashboard" $false "Failed to fetch date-filtered dashboard: $($_.Exception.Message)"
}

# Step 25: Paginated Payment History
Write-Host "25. Fetching Paginated Payment Ledger (GET /reports/payments?page=0&size=5)..." -ForegroundColor Yellow
try {
    $pmtHistory = Invoke-RestMethod -Uri "$baseUrl/reports/payments?page=0&size=5" -Method Get -ErrorAction Stop
    Assert-Result "Paginated Payment History" ($pmtHistory.content.Count -ge 1 -and $pmtHistory.totalElements -ge 2) "Retrieved $($pmtHistory.content.Count) payments on page 0 (Total ledger entries: $($pmtHistory.totalElements))"

    Write-Host "`nRecent Payments Sample:" -ForegroundColor Cyan
    $pmtHistory.content | Format-Table paymentNumber, paymentMethodType, amount, paymentDate, transactionReference -AutoSize
} catch {
    Assert-Result "Paginated Payment History" $false "Failed to fetch paginated payments: $($_.Exception.Message)"
}

# -----------------------------------------------------------------------------
# FINAL SUMMARY
# -----------------------------------------------------------------------------
Print-Header "TEST SUMMARY"
Write-Host "Total Passed : $global:passedCount" -ForegroundColor Green
Write-Host "Total Failed : $global:failedCount" -ForegroundColor Red
Write-Host "Total Skipped: $global:skippedCount" -ForegroundColor Yellow

if ($global:failedCount -gt 0) {
    Write-Host "`n[OVERALL RESULT: FAILED] Some workflow requirements did not pass." -ForegroundColor Red
    exit 1
} else {
    Write-Host "`n[OVERALL RESULT: SUCCESS] All Member 4 End-to-End Workflows Verified Successfully!" -ForegroundColor Green
    exit 0
}
