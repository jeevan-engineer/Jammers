package com.brightloop.dto;

import com.brightloop.enums.SubscriptionStatus;
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
 * Full details of a subscription for client responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {

    private Long id;
    private String subscriptionNumber;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private Long quotationTemplateId;
    private String quotationTemplateName;
    private Long paymentMethodId;
    private SubscriptionStatus status;
    private String billingInterval;
    private Integer intervalCount;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate trialStartDate;
    private LocalDate trialEndDate;
    private LocalDate currentPeriodStart;
    private LocalDate currentPeriodEnd;
    private LocalDate nextBillingDate;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private LocalDateTime pausedAt;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String currency;
    private Boolean autoRenew;
    private String terms;
    private String notes;

    @Builder.Default
    private List<SubscriptionLineDto> lines = new ArrayList<>();

    @Builder.Default
    private List<StatusHistoryDto> statusHistories = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusHistoryDto {
        private Long id;
        private SubscriptionStatus previousStatus;
        private SubscriptionStatus newStatus;
        private LocalDateTime changedAt;
        private String changedBy;
        private String reason;
        private String comment;
    }
}
