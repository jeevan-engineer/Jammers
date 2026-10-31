package com.brightloop;

import com.brightloop.dto.InvoiceCreateRequest;
import com.brightloop.dto.InvoiceLineDto;
import com.brightloop.dto.PaymentRecordRequest;
import com.brightloop.dto.QuotationTemplateDto;
import com.brightloop.dto.SubscriptionCreateRequest;
import com.brightloop.dto.SubscriptionLineDto;
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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}
