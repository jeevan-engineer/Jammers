package com.brightloop.dto;

import com.brightloop.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Full details of an invoice for client responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private Long id;
    private String invoiceNumber;
    private Long subscriptionId;
    private String subscriptionNumber;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String billingAddress;
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;
    private InvoiceStatus status;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDateTime paidAt;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal amountDue;
    private String currency;
    private String notes;
    private String terms;

    @Builder.Default
    private List<InvoiceLineDto> lines = new ArrayList<>();

    @Builder.Default
    private List<PaymentResponse> payments = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
