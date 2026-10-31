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
import com.brightloop.repository.InvoiceRepository;
import com.brightloop.repository.PaymentRepository;
import com.brightloop.repository.QuotationTemplateRepository;
import com.brightloop.repository.SubscriptionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuotationTemplateRepository templateRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    // Static variables shared across ordered lifecycle test steps
    private static Long templateId;
    private static Long subscriptionId;
    private static Long invoice10kId;

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
                .name("Standard Enterprise SaaS Plan")
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
                .andExpect(jsonPath("$.name", is("Standard Enterprise SaaS Plan")))
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
                .customerId(501L)
                .customerName("Global Tech Corp")
                .customerEmail("billing@globaltech.com")
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
                                .reason("Customer accepted quote")
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
        // Line calculation:
        // Quantity = 2.00, UnitPrice = 100.00 -> Gross = 200.00
        // Discount = 10% -> 20.00 -> Subtotal = 180.00
        // Tax = 18% -> 180 * 0.18 = 32.40
        // Total = 180 + 32.40 = 212.40
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
                .customerId(501L)
                .customerName("Global Tech Corp")
                .customerEmail("billing@globaltech.com")
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
        // Attempting to generate another invoice for the same subscription and period [2026-11-01 to 2026-11-30]
        InvoiceLineDto line = InvoiceLineDto.builder()
                .productId(301L)
                .productName("Duplicate Attempt")
                .quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal("100.00"))
                .build();

        InvoiceCreateRequest duplicateRequest = InvoiceCreateRequest.builder()
                .subscriptionId(subscriptionId)
                .customerId(501L)
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
                .customerId(701L)
                .customerName("Nexus Enterprises")
                .customerEmail("nexus@corp.com")
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
                .customerId(701L)
                .amount(new BigDecimal("4000.00"))
                .currency("INR")
                .transactionReference("TXN-PARTIAL-4000")
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
                .customerId(701L)
                .amount(new BigDecimal("6000.00"))
                .currency("INR")
                .transactionReference("TXN-SETTLE-6000")
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
                .customerId(701L)
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
