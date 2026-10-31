package com.brightloop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data representation of a processed or recorded payment transaction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private String paymentNumber;
    private Long invoiceId;
    private String invoiceNumber;
    private Long paymentMethodId;
    private String paymentMethodType;
    private Long customerId;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime paymentDate;
    private String status;
    private String transactionReference;
    private String gatewayResponse;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
