package com.brightloop.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payload to record a payment transaction against an invoice.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRecordRequest {

    @NotNull(message = "Invoice ID is required")
    private Long invoiceId;

    private Long paymentMethodId;

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    private BigDecimal amount;

    @Builder.Default
    private String currency = "USD";

    private LocalDateTime paymentDate;
    private String transactionReference;
    private String gatewayResponse;
    private String notes;
}
