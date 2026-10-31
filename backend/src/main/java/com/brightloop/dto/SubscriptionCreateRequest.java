package com.brightloop.dto;

import com.brightloop.enums.SubscriptionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Payload to create a new subscription or quotation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionCreateRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    private String customerName;
    private String customerEmail;

    private Long quotationTemplateId;
    private Long paymentMethodId;

    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.DRAFT;

    @Builder.Default
    private String billingInterval = "MONTHLY";

    @Builder.Default
    private Integer intervalCount = 1;

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate trialStartDate;
    private LocalDate trialEndDate;

    @Builder.Default
    private String currency = "USD";

    @Builder.Default
    private Boolean autoRenew = true;

    private String terms;
    private String notes;

    @Valid
    @NotEmpty(message = "Subscription must have at least one line item")
    @Builder.Default
    private List<SubscriptionLineDto> lines = new ArrayList<>();
}
