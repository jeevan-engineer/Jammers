package com.brightloop;

import com.brightloop.dto.InvoiceCreateRequest;
import com.brightloop.dto.InvoiceLineDto;
import com.brightloop.dto.PaymentRecordRequest;
import com.brightloop.dto.QuotationGenerateRequest;
import com.brightloop.dto.QuotationTemplateDto;
import com.brightloop.dto.SubscriptionCreateRequest;
import com.brightloop.dto.SubscriptionLineDto;
import com.brightloop.dto.SubscriptionRenewalRequest;
import com.brightloop.dto.SubscriptionStatusUpdateRequest;
import com.brightloop.enums.InvoiceStatus;
import com.brightloop.enums.SubscriptionStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test suite verifying all 4 Member 4 core scenarios:
 * Scenario A: Subscription Lifecycle & Quotation Template
 * Scenario B: Invoice Generation & Anti-Double-Billing Guard
 * Scenario C: Payment Reconciliation (The ₹10,000 Financial Flow)
 * Scenario D: Real-Time Financial Dashboard Reporting
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SubscriptionBillingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Shared across ordered test steps
    private static Long templateId;
    private static Long subscriptionId;
    private static Long invoice10kId;
    private static final long RUN_ID = System.currentTimeMillis() % 100000;

    // -------------------------------------------------------------------------
    // SCENARIO A: Subscription Lifecycle & Quotation Template
    // -------------------------------------------------------------------------
    @Test
    @Order(1)
    @DisplayName("Scenario A.1: Create Quotation Template with default line items")
    void testCreateQuotationTemplate() throws Exception {
        QuotationTemplateDto.LineDto line1 = QuotationTemplateDto.LineDto.builder()
                .productId(101L)
                .productName("SaaS Enterprise Tier")
                .description("Monthly recurring subscription tier")
                .quantity(new BigDecimal("1.00"))
                .unitPrice(new BigDecimal("2500.00"))
                .discount(BigDecimal.ZERO)
                .sequence(1)
                .build();

        QuotationTemplateDto templateRequest = QuotationTemplateDto.builder()
                .name("Enterprise SaaS Plan " + RUN_ID)
                .description("Preconfigured 12-month recurring template")
                .recurringInterval("MONTHLY")
                .duration(12)
                .durationUnit("MONTHS")
                .autoClose(false)
                .active(true)
                .lines(List.of(line1))
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/quotation-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(templateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Enterprise SaaS Plan " + RUN_ID)))
                .andExpect(jsonPath("$.lines", hasSize(1)))
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());
        templateId = responseJson.get("id").asLong();
        assertNotNull(templateId);
    }

    @Test
    @Order(2)
    @DisplayName("Scenario A.2: Create Subscription in DRAFT status with unique SUB- number")
    void testCreateSubscription() throws Exception {
        SubscriptionLineDto line = SubscriptionLineDto.builder()
                .productId(101L)
                .productName("SaaS Enterprise Tier")
                .description("Monthly enterprise seat")
                .quantity(new BigDecimal("2.00"))
                .unitPrice(new BigDecimal("2500.00"))
                .discount(BigDecimal.ZERO)
                .taxRate(new BigDecimal("18.00"))
                .build();

        SubscriptionCreateRequest request = SubscriptionCreateRequest.builder()
                .customerId(501L + RUN_ID)
                .customerName("Global Tech Corp " + RUN_ID)
                .customerEmail("billing" + RUN_ID + "@globaltech.com")
                .quotationTemplateId(templateId)
                .billingInterval("MONTHLY")
                .intervalCount(1)
                .startDate(LocalDate.now())
                .currency("INR")
                .lines(List.of(line))
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.subscriptionNumber", startsWith("SUB-")))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andExpect(jsonPath("$.subtotal", is(5000.00)))
                .andExpect(jsonPath("$.taxAmount", is(900.00)))
                .andExpect(jsonPath("$.totalAmount", is(5900.00)))
                .andExpect(jsonPath("$.statusHistories", hasSize(1)))
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());
        subscriptionId = responseJson.get("id").asLong();
        assertNotNull(subscriptionId);
    }

    @Test
    @Order(3)
    @DisplayName("Scenario A.3: Advance Lifecycle DRAFT -> QUOTATION -> CONFIRMED -> ACTIVE")
    void testAdvanceSubscriptionLifecycle() throws Exception {
        // Step 1: DRAFT -> QUOTATION
        mockMvc.perform(patch("/api/v1/subscriptions/" + subscriptionId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SubscriptionStatusUpdateRequest.builder()
                                .status(SubscriptionStatus.QUOTATION)
                                .reason("Sales quotation generated and sent to customer")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("QUOTATION")));

        // Step 2: QUOTATION -> CONFIRMED
        mockMvc.perform(patch("/api/v1/subscriptions/" + subscriptionId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SubscriptionStatusUpdateRequest.builder()
                                .status(SubscriptionStatus.CONFIRMED)
                                .reason("Quote approved by customer")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        // Step 3: CONFIRMED -> ACTIVE
        mockMvc.perform(patch("/api/v1/subscriptions/" + subscriptionId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SubscriptionStatusUpdateRequest.builder()
                                .status(SubscriptionStatus.ACTIVE)
                                .reason("Contract activated")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.statusHistories", hasSize(4))); // DRAFT + 3 transitions
    }

    // -------------------------------------------------------------------------
    // SCENARIO B: Invoice Generation & Anti-Double-Billing Guard
    // -------------------------------------------------------------------------
    @Test
    @Order(4)
    @DisplayName("Scenario B.1: Generate Invoice with exact financial math")
    void testCreateInvoiceWithCalculations() throws Exception {
        InvoiceLineDto line = InvoiceLineDto.builder()
                .productId(301L)
                .productName("Software License Add-on")
                .quantity(new BigDecimal("2.00"))
                .unitPrice(new BigDecimal("100.00"))
                .discount(new BigDecimal("10.00"))
                .taxRate(new BigDecimal("18.00"))
                .build();

        InvoiceCreateRequest request = InvoiceCreateRequest.builder()
                .subscriptionId(subscriptionId)
                .customerId(501L + RUN_ID)
                .customerName("Global Tech Corp " + RUN_ID)
                .customerEmail("billing" + RUN_ID + "@globaltech.com")
                .billingPeriodStart(LocalDate.of(2026, 11, 1))
                .billingPeriodEnd(LocalDate.of(2026, 11, 30))
                .status(InvoiceStatus.UNPAID)
                .currency("INR")
                .lines(List.of(line))
                .build();

        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber", startsWith("INV-")))
                .andExpect(jsonPath("$.status", is("UNPAID")))
                .andExpect(jsonPath("$.subtotal", is(180.00)))
                .andExpect(jsonPath("$.taxAmount", is(32.40)))
                .andExpect(jsonPath("$.discountAmount", is(20.00)))
                .andExpect(jsonPath("$.totalAmount", is(212.40)))
                .andExpect(jsonPath("$.amountDue", is(212.40)))
                .andExpect(jsonPath("$.amountPaid", is(0.00)));
    }

    @Test
    @Order(5)
    @DisplayName("Scenario B.2: Anti-Double-Billing Guard blocks duplicate interval")
    void testAntiDoubleBillingGuard() throws Exception {
        InvoiceLineDto line = InvoiceLineDto.builder()
                .productId(301L)
                .productName("Duplicate Attempt")
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("100.00"))
                .build();

        InvoiceCreateRequest duplicateRequest = InvoiceCreateRequest.builder()
                .subscriptionId(subscriptionId)
                .customerId(501L + RUN_ID)
                .billingPeriodStart(LocalDate.of(2026, 11, 1))
                .billingPeriodEnd(LocalDate.of(2026, 11, 30))
                .lines(List.of(line))
                .build();

        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    // -------------------------------------------------------------------------
    // SCENARIO C: Payment Reconciliation (The ₹10,000 Financial Flow)
    // -------------------------------------------------------------------------
    @Test
    @Order(6)
    @DisplayName("Scenario C.1: Create invoice for ₹10,000")
    void testCreateInvoiceFor10k() throws Exception {
        InvoiceLineDto line = InvoiceLineDto.builder()
                .productId(999L)
                .productName("Consulting & Platform License")
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("10000.00"))
                .build();

        InvoiceCreateRequest request = InvoiceCreateRequest.builder()
                .customerId(701L + RUN_ID)
                .customerName("Nexus Enterprises " + RUN_ID)
                .customerEmail("nexus" + RUN_ID + "@corp.com")
                .billingPeriodStart(LocalDate.of(2026, 12, 1))
                .billingPeriodEnd(LocalDate.of(2026, 12, 31))
                .status(InvoiceStatus.UNPAID)
                .currency("INR")
                .lines(List.of(line))
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount", is(10000.00)))
                .andExpect(jsonPath("$.amountPaid", is(0.00)))
                .andExpect(jsonPath("$.amountDue", is(10000.00)))
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());
        invoice10kId = responseJson.get("id").asLong();
        assertNotNull(invoice10kId);
    }

    @Test
    @Order(7)
    @DisplayName("Scenario C.2: Record Partial Payment (₹4,000) -> PARTIALLY_PAID")
    void testRecordPartialPayment() throws Exception {
        PaymentRecordRequest request = PaymentRecordRequest.builder()
                .invoiceId(invoice10kId)
                .customerId(701L + RUN_ID)
                .amount(new BigDecimal("4000.00"))
                .currency("INR")
                .transactionReference("TXN-PARTIAL-" + RUN_ID)
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentNumber", startsWith("PAY-")))
                .andExpect(jsonPath("$.amount", is(4000.00)))
                .andExpect(jsonPath("$.status", is("SUCCESS")));

        // Verify updated invoice state
        mockMvc.perform(get("/api/v1/invoices/" + invoice10kId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountPaid", is(4000.00)))
                .andExpect(jsonPath("$.amountDue", is(6000.00)))
                .andExpect(jsonPath("$.status", is("PARTIALLY_PAID")));
    }

    @Test
    @Order(8)
    @DisplayName("Scenario C.3: Record Full Settlement (₹6,000) -> PAID")
    void testRecordFullSettlement() throws Exception {
        PaymentRecordRequest request = PaymentRecordRequest.builder()
                .invoiceId(invoice10kId)
                .customerId(701L + RUN_ID)
                .amount(new BigDecimal("6000.00"))
                .currency("INR")
                .transactionReference("TXN-SETTLE-" + RUN_ID)
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount", is(6000.00)))
                .andExpect(jsonPath("$.status", is("SUCCESS")));

        // Verify invoice is completely settled
        mockMvc.perform(get("/api/v1/invoices/" + invoice10kId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountPaid", is(10000.00)))
                .andExpect(jsonPath("$.amountDue", is(0.00)))
                .andExpect(jsonPath("$.status", is("PAID")))
                .andExpect(jsonPath("$.paidAt", notNullValue()));
    }

    @Test
    @Order(9)
    @DisplayName("Scenario C.4: Overpayment Attempt Guard throws Bad Request")
    void testOverpaymentAttemptGuard() throws Exception {
        PaymentRecordRequest overpaymentRequest = PaymentRecordRequest.builder()
                .invoiceId(invoice10kId)
                .customerId(701L + RUN_ID)
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overpaymentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    // -------------------------------------------------------------------------
    // SCENARIO D: Real-Time Financial Dashboard
    // -------------------------------------------------------------------------
    @Test
    @Order(10)
    @DisplayName("Scenario D.1: Fetch Dashboard Metrics and verify real-time aggregates")
    void testDashboardReporting() throws Exception {
        mockMvc.perform(get("/api/v1/reports/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeSubscriptions", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalRevenue", greaterThanOrEqualTo(10000.00)))
                .andExpect(jsonPath("$.recentActivities", notNullValue()));
    }

    // -------------------------------------------------------------------------
    // ADDITIONAL LIFECYCLE, BILLING, PAYMENT & BENCHMARK TESTS (REQUIREMENTS AUDIT)
    // -------------------------------------------------------------------------
    @Test
    @Order(11)
    @DisplayName("Scenario E.1: Reject Invalid Subscription Lifecycle Transition (ACTIVE -> QUOTATION)")
    void testInvalidSubscriptionLifecycleTransition() throws Exception {
        mockMvc.perform(patch("/api/v1/subscriptions/" + subscriptionId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SubscriptionStatusUpdateRequest.builder()
                                .status(SubscriptionStatus.QUOTATION)
                                .reason("Illegal regression attempt")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @Order(12)
    @DisplayName("Scenario E.2: Generate Quotation from Template (POST /api/v1/quotation-templates/{id}/generate-quotation)")
    void testGenerateQuotationFromTemplate() throws Exception {
        QuotationGenerateRequest request = QuotationGenerateRequest.builder()
                .customerId(801L + RUN_ID)
                .customerName("Acme Proposal Corp " + RUN_ID)
                .customerEmail("proposals" + RUN_ID + "@acme.com")
                .startDate(LocalDate.now())
                .validityDays(14)
                .currency("INR")
                .notes("Generated formal quote")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/quotation-templates/" + templateId + "/generate-quotation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subscriptionNumber", startsWith("SUB-")))
                .andExpect(jsonPath("$.status", is("QUOTATION")))
                .andExpect(jsonPath("$.quoteValidUntil", notNullValue()))
                .andExpect(jsonPath("$.lines", hasSize(greaterThanOrEqualTo(1))))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        Long generatedQuoteId = json.get("id").asLong();
        assertNotNull(generatedQuoteId);
    }

    @Test
    @Order(13)
    @DisplayName("Scenario E.3: Prevent Expired Quotation from being Confirmed")
    void testExpiredQuotationCannotBeConfirmed() throws Exception {
        // Create quotation explicitly with expired quoteValidUntil in the past
        SubscriptionLineDto line = SubscriptionLineDto.builder()
                .productName("Consulting Seat")
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("1000.00"))
                .build();

        SubscriptionCreateRequest request = SubscriptionCreateRequest.builder()
                .customerId(802L + RUN_ID)
                .status(SubscriptionStatus.QUOTATION)
                .startDate(LocalDate.now().minusDays(30))
                .quoteValidUntil(LocalDate.now().minusDays(5)) // Expired 5 days ago
                .lines(List.of(line))
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("QUOTATION")))
                .andReturn();

        Long expiredSubId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        // Attempt transition to CONFIRMED -> must be rejected
        mockMvc.perform(patch("/api/v1/subscriptions/" + expiredSubId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(SubscriptionStatusUpdateRequest.builder()
                                .status(SubscriptionStatus.CONFIRMED)
                                .reason("Attempting to accept expired quote")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @Order(14)
    @DisplayName("Scenario E.4: Subscription Pause and Resume Lifecycle")
    void testSubscriptionPauseAndResume() throws Exception {
        // Pause active subscription
        mockMvc.perform(post("/api/v1/subscriptions/" + subscriptionId + "/pause?reason=CustomerRequestedHold"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pausedAt", notNullValue()));

        // Resume active subscription
        mockMvc.perform(post("/api/v1/subscriptions/" + subscriptionId + "/resume"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pausedAt").doesNotExist());
    }

    @Test
    @Order(15)
    @DisplayName("Scenario E.5: Duplicate Pause / Invalid Resume Guards")
    void testDuplicatePauseRejection() throws Exception {
        // Pause once
        mockMvc.perform(post("/api/v1/subscriptions/" + subscriptionId + "/pause?reason=Maintenance"))
                .andExpect(status().isOk());

        // Attempt second pause -> must fail with 400
        mockMvc.perform(post("/api/v1/subscriptions/" + subscriptionId + "/pause?reason=DuplicateHold"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")));

        // Resume back to clean active state
        mockMvc.perform(post("/api/v1/subscriptions/" + subscriptionId + "/resume"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(16)
    @DisplayName("Scenario E.6: Subscription Renewal with audit log")
    void testSubscriptionRenewal() throws Exception {
        SubscriptionRenewalRequest renewalReq = SubscriptionRenewalRequest.builder()
                .duration(12)
                .durationUnit("MONTHS")
                .autoRenew(true)
                .notes("Annual contract renewal executed")
                .build();

        mockMvc.perform(post("/api/v1/subscriptions/" + subscriptionId + "/renew")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(renewalReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endDate", notNullValue()))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @Order(17)
    @DisplayName("Scenario E.7: Invalid Renewal Date Guard rejects past / duplicate renewal")
    void testInvalidRenewalRejection() throws Exception {
        SubscriptionRenewalRequest invalidReq = SubscriptionRenewalRequest.builder()
                .newEndDate(LocalDate.now().minusDays(10)) // End date in past
                .build();

        mockMvc.perform(post("/api/v1/subscriptions/" + subscriptionId + "/renew")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @Order(18)
    @DisplayName("Scenario E.8: Single Subscription Invoice Generation (POST /api/v1/subscriptions/{id}/generate-invoice)")
    void testSingleInvoiceGeneration() throws Exception {
        // Create an active subscription ready for single invoice billing
        SubscriptionLineDto line = SubscriptionLineDto.builder()
                .productName("SaaS Add-on Single")
                .quantity(new BigDecimal("1.00"))
                .unitPrice(new BigDecimal("1500.00"))
                .taxRate(new BigDecimal("18.00"))
                .build();

        SubscriptionCreateRequest req = SubscriptionCreateRequest.builder()
                .customerId(901L + RUN_ID)
                .status(SubscriptionStatus.ACTIVE)
                .billingInterval("MONTHLY")
                .startDate(LocalDate.of(2027, 5, 1))
                .lines(List.of(line))
                .build();

        MvcResult subRes = mockMvc.perform(post("/api/v1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long singleSubId = objectMapper.readTree(subRes.getResponse().getContentAsString()).get("id").asLong();

        // Trigger single invoice generation
        mockMvc.perform(post("/api/v1/subscriptions/" + singleSubId + "/generate-invoice"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.invoiceId", notNullValue()));

        // Duplicate attempt on same period must fail
        mockMvc.perform(post("/api/v1/subscriptions/" + singleSubId + "/generate-invoice"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @Order(19)
    @DisplayName("Scenario E.9: Batch Invoice Generation for Due Subscriptions (Phase 4 Full Audit)")
    void testBatchInvoiceGeneration() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate lastMonth = today.minusMonths(1);

        SubscriptionLineDto line = SubscriptionLineDto.builder()
                .productName("Batch Test Compute")
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("7500.00"))
                .taxRate(new BigDecimal("18.00"))
                .build();

        // 1. Eligible active subscription due for billing today
        SubscriptionCreateRequest eligibleReq = SubscriptionCreateRequest.builder()
                .customerId(920L + RUN_ID)
                .customerName("Batch Due Client")
                .status(SubscriptionStatus.ACTIVE)
                .billingInterval("MONTHLY")
                .startDate(lastMonth)
                .lines(List.of(line))
                .build();

        MvcResult eligibleRes = mockMvc.perform(post("/api/v1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eligibleReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long eligibleSubId = objectMapper.readTree(eligibleRes.getResponse().getContentAsString()).get("id").asLong();

        // 2. Paused subscription due for billing today
        SubscriptionCreateRequest pausedReq = SubscriptionCreateRequest.builder()
                .customerId(921L + RUN_ID)
                .customerName("Paused Client")
                .status(SubscriptionStatus.ACTIVE)
                .billingInterval("MONTHLY")
                .startDate(lastMonth)
                .lines(List.of(line))
                .build();

        MvcResult pausedRes = mockMvc.perform(post("/api/v1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pausedReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long pausedSubId = objectMapper.readTree(pausedRes.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(post("/api/v1/subscriptions/" + pausedSubId + "/pause")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Maintenance\"}"))
                .andExpect(status().isOk());

        // 3. Closed subscription due for billing today
        SubscriptionCreateRequest closedReq = SubscriptionCreateRequest.builder()
                .customerId(922L + RUN_ID)
                .customerName("Closed Client")
                .status(SubscriptionStatus.ACTIVE)
                .billingInterval("MONTHLY")
                .startDate(lastMonth)
                .lines(List.of(line))
                .build();

        MvcResult closedRes = mockMvc.perform(post("/api/v1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closedReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long closedSubId = objectMapper.readTree(closedRes.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(post("/api/v1/subscriptions/" + closedSubId + "/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Terminated\"}"))
                .andExpect(status().isOk());

        // 4. Future-dated subscription (starts in future, nextBillingDate in future)
        SubscriptionCreateRequest futureReq = SubscriptionCreateRequest.builder()
                .customerId(923L + RUN_ID)
                .customerName("Future Client")
                .status(SubscriptionStatus.ACTIVE)
                .billingInterval("MONTHLY")
                .startDate(today.plusMonths(2))
                .lines(List.of(line))
                .build();

        MvcResult futureRes = mockMvc.perform(post("/api/v1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(futureReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long futureSubId = objectMapper.readTree(futureRes.getResponse().getContentAsString()).get("id").asLong();

        // 5. Execute batch invoice generation
        MvcResult batchRes1 = mockMvc.perform(post("/api/v1/subscriptions/generate-billing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.invoicesGeneratedCount", greaterThanOrEqualTo(1)))
                .andReturn();

        JsonNode batchJson1 = objectMapper.readTree(batchRes1.getResponse().getContentAsString());
        assertTrue(batchJson1.get("invoicesGeneratedCount").asInt() >= 1, "Must generate at least 1 invoice for the eligible subscription");

        // 6. Verify eligible subscription has an invoice created
        MvcResult subInvoicesRes = mockMvc.perform(get("/api/v1/invoices/subscription/" + eligibleSubId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].subtotal", is(7500.00)))
                .andExpect(jsonPath("$[0].totalAmount", is(8850.00)))
                .andReturn();

        // 7. Verify nextBillingDate has advanced
        MvcResult refreshedSubRes = mockMvc.perform(get("/api/v1/subscriptions/" + eligibleSubId))
                .andExpect(status().isOk())
                .andReturn();
        String nextBillingDateStr = objectMapper.readTree(refreshedSubRes.getResponse().getContentAsString()).get("nextBillingDate").asText();
        LocalDate nextBilling = LocalDate.parse(nextBillingDateStr);
        assertTrue(nextBilling.isAfter(today), "Next billing date must have advanced into the future");

        // 8. Verify paused, closed, and future subscriptions were NOT billed
        mockMvc.perform(get("/api/v1/invoices/subscription/" + pausedSubId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/v1/invoices/subscription/" + closedSubId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/v1/invoices/subscription/" + futureSubId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // 9. Execute batch billing again immediately - duplicate invoices must NOT be generated
        MvcResult batchRes2 = mockMvc.perform(post("/api/v1/subscriptions/generate-billing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andReturn();

        // Check that eligible subscription still has only 1 invoice
        mockMvc.perform(get("/api/v1/invoices/subscription/" + eligibleSubId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @Order(20)
    @DisplayName("Scenario E.10: Invoice Lifecycle - Confirm and Generate Printable Invoice")
    void testInvoiceConfirmationAndPrintable() throws Exception {
        // Create DRAFT invoice
        InvoiceLineDto line = InvoiceLineDto.builder()
                .productName("Service Retainer")
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("3000.00"))
                .build();

        InvoiceCreateRequest req = InvoiceCreateRequest.builder()
                .customerId(950L + RUN_ID)
                .customerName("Retainer Client")
                .status(InvoiceStatus.DRAFT)
                .billingPeriodStart(LocalDate.of(2028, 2, 1))
                .billingPeriodEnd(LocalDate.of(2028, 2, 28))
                .lines(List.of(line))
                .build();

        MvcResult invRes = mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andReturn();

        Long draftInvId = objectMapper.readTree(invRes.getResponse().getContentAsString()).get("id").asLong();

        // Confirm invoice
        mockMvc.perform(post("/api/v1/invoices/" + draftInvId + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        // Printable text receipt
        MvcResult printRes = mockMvc.perform(get("/api/v1/invoices/" + draftInvId + "/print"))
                .andExpect(status().isOk())
                .andReturn();

        String printable = printRes.getResponse().getContentAsString();
        assertTrue(printable.contains("BRIGHTLOOP INVOICE RECEIPT"));
        assertTrue(printable.contains("Service Retainer"));
    }

    @Test
    @Order(21)
    @DisplayName("Scenario E.11: Cancel Eligible Unsettled Invoice")
    void testInvoiceCancellation() throws Exception {
        InvoiceLineDto line = InvoiceLineDto.builder()
                .productName("Consulting Ticket")
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("500.00"))
                .build();

        InvoiceCreateRequest req = InvoiceCreateRequest.builder()
                .customerId(960L + RUN_ID)
                .billingPeriodStart(LocalDate.of(2028, 3, 1))
                .billingPeriodEnd(LocalDate.of(2028, 3, 31))
                .lines(List.of(line))
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long cancelInvId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/v1/invoices/" + cancelInvId + "/cancel?reason=ClientCancelledScope"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    @Order(22)
    @DisplayName("Scenario E.12: Cannot Cancel Settled / PAID Invoice")
    void testCannotCancelPaidInvoice() throws Exception {
        mockMvc.perform(post("/api/v1/invoices/" + invoice10kId + "/cancel?reason=FraudulentAttempt"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @Order(23)
    @DisplayName("Scenario E.13: Dispatch Invoice Notification (Boundary Interface)")
    void testSendInvoiceNotification() throws Exception {
        mockMvc.perform(post("/api/v1/invoices/" + invoice10kId + "/send"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @Order(24)
    @DisplayName("Scenario E.14: Reject Zero or Negative Payment Amounts")
    void testZeroAndNegativePaymentRejection() throws Exception {
        // Zero payment
        PaymentRecordRequest zeroReq = PaymentRecordRequest.builder()
                .invoiceId(invoice10kId)
                .customerId(701L + RUN_ID)
                .amount(BigDecimal.ZERO)
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroReq)))
                .andExpect(status().isBadRequest());

        // Negative payment
        PaymentRecordRequest negReq = PaymentRecordRequest.builder()
                .invoiceId(invoice10kId)
                .customerId(701L + RUN_ID)
                .amount(new BigDecimal("-100.00"))
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(25)
    @DisplayName("Scenario E.15: Duplicate Payment Reference Rejection")
    void testDuplicatePaymentReferenceRejection() throws Exception {
        // Create an unpaid invoice
        InvoiceLineDto line = InvoiceLineDto.builder()
                .productName("Hardware Component")
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("2000.00"))
                .build();

        InvoiceCreateRequest invReq = InvoiceCreateRequest.builder()
                .customerId(970L + RUN_ID)
                .billingPeriodStart(LocalDate.of(2028, 4, 1))
                .billingPeriodEnd(LocalDate.of(2028, 4, 30))
                .lines(List.of(line))
                .build();

        MvcResult invRes = mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long invId = objectMapper.readTree(invRes.getResponse().getContentAsString()).get("id").asLong();
        String uniqueTxnRef = "TXN-DUP-TEST-" + RUN_ID;

        // Payment 1 with unique ref
        PaymentRecordRequest pmt1 = PaymentRecordRequest.builder()
                .invoiceId(invId)
                .customerId(970L + RUN_ID)
                .amount(new BigDecimal("500.00"))
                .transactionReference(uniqueTxnRef)
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pmt1)))
                .andExpect(status().isCreated());

        // Payment 2 with duplicate ref -> must fail
        PaymentRecordRequest pmt2 = PaymentRecordRequest.builder()
                .invoiceId(invId)
                .customerId(970L + RUN_ID)
                .amount(new BigDecimal("500.00"))
                .transactionReference(uniqueTxnRef)
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pmt2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @Order(26)
    @DisplayName("Scenario E.16: Reject Payment Against CANCELLED Invoice")
    void testPaymentAgainstCancelledInvoice() throws Exception {
        // Create and cancel invoice
        InvoiceLineDto line = InvoiceLineDto.builder()
                .productName("Voided Item")
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("800.00"))
                .build();

        InvoiceCreateRequest invReq = InvoiceCreateRequest.builder()
                .customerId(980L + RUN_ID)
                .billingPeriodStart(LocalDate.of(2028, 5, 1))
                .billingPeriodEnd(LocalDate.of(2028, 5, 31))
                .lines(List.of(line))
                .build();

        MvcResult invRes = mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long invId = objectMapper.readTree(invRes.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(post("/api/v1/invoices/" + invId + "/cancel?reason=Mistake"))
                .andExpect(status().isOk());

        // Attempt payment -> must fail
        PaymentRecordRequest pmt = PaymentRecordRequest.builder()
                .invoiceId(invId)
                .customerId(980L + RUN_ID)
                .amount(new BigDecimal("400.00"))
                .build();

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pmt)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @Order(27)
    @DisplayName("Scenario E.17: Concurrent Payment & Overpayment Protection (Pessimistic Lock)")
    void testConcurrentPaymentProtection() throws Exception {
        // Create an invoice with balance ₹5,000
        InvoiceLineDto line = InvoiceLineDto.builder()
                .productName("Concurrent Test Item")
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("5000.00"))
                .build();

        InvoiceCreateRequest invReq = InvoiceCreateRequest.builder()
                .customerId(990L + RUN_ID)
                .billingPeriodStart(LocalDate.of(2028, 6, 1))
                .billingPeriodEnd(LocalDate.of(2028, 6, 30))
                .lines(List.of(line))
                .build();

        MvcResult invRes = mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long concInvId = objectMapper.readTree(invRes.getResponse().getContentAsString()).get("id").asLong();

        // Concurrently attempt 2 payments of ₹3,000 each (Total ₹6,000, which exceeds ₹5,000 balance)
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger errorCount = new java.util.concurrent.atomic.AtomicInteger(0);

        java.util.concurrent.Callable<Void> task = () -> {
            try {
                PaymentRecordRequest pmt = PaymentRecordRequest.builder()
                        .invoiceId(concInvId)
                        .customerId(990L + RUN_ID)
                        .amount(new BigDecimal("3000.00"))
                        .transactionReference("CONC-" + UUID.randomUUID())
                        .build();

                MvcResult r = mockMvc.perform(post("/api/v1/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(pmt)))
                        .andReturn();

                if (r.getResponse().getStatus() == 201) {
                    successCount.incrementAndGet();
                } else if (r.getResponse().getStatus() == 400) {
                    errorCount.incrementAndGet();
                }
            } catch (Exception e) {
                errorCount.incrementAndGet();
            }
            return null;
        };

        executor.invokeAll(List.of(task, task));
        executor.shutdown();
        executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        // Exactly one should succeed (₹3,000 applied, ₹2,000 due) and one must be rejected (since ₹3,000 > ₹2,000)
        assertTrue(successCount.get() == 1, "Exactly one concurrent payment should succeed");
        assertTrue(errorCount.get() == 1, "The second concurrent payment must be rejected for exceeding balance");
    }

    @Test
    @Order(28)
    @DisplayName("Scenario E.18: Dashboard Metrics with Date Filters")
    void testDashboardDateFiltering() throws Exception {
        mockMvc.perform(get("/api/v1/reports/dashboard?startDate=2026-01-01&endDate=2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeSubscriptions", notNullValue()))
                .andExpect(jsonPath("$.totalRevenue", notNullValue()));
    }

    @Test
    @Order(29)
    @DisplayName("Scenario E.19: Paginated Payment Transaction History (GET /api/v1/reports/payments)")
    void testDashboardPaymentHistoryPagination() throws Exception {
        mockMvc.perform(get("/api/v1/reports/payments?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(30)
    @DisplayName("Scenario E.20: Dashboard Performance Benchmark (< 2.0s response time)")
    void testDashboardPerformanceBenchmark() throws Exception {
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/api/v1/reports/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSubscriptions", notNullValue()));

        long elapsedTime = System.currentTimeMillis() - startTime;
        assertTrue(elapsedTime < 2000, "Dashboard metrics response time must be under 2000ms. Actual: " + elapsedTime + "ms");
    }

    @Test
    @Order(31)
    @DisplayName("Scenario E.21: Subscription Early Closure (POST /api/v1/subscriptions/{id}/close)")
    void testEarlyClosure() throws Exception {
        mockMvc.perform(post("/api/v1/subscriptions/" + subscriptionId + "/close?reason=EarlyContractTermination"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CLOSED")))
                .andExpect(jsonPath("$.cancelledAt", notNullValue()));
    }
}
