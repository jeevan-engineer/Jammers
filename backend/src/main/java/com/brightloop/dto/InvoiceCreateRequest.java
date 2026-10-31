package com.brightloop.dto;

import com.brightloop.enums.InvoiceStatus;
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
 * Request payload to manually generate an invoice or create an ad-hoc bill.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCreateRequest {

    private Long subscriptionId;

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    private String customerName;
    private String customerEmail;
    private String billingAddress;

    @NotNull(message = "Billing period start date is required")
    private LocalDate billingPeriodStart;

    @NotNull(message = "Billing period end date is required")
    private LocalDate billingPeriodEnd;

    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    private LocalDate issueDate;
    private LocalDate dueDate;

    @Builder.Default
    private String currency = "USD";

    private String notes;
    private String terms;

    @Valid
    @NotEmpty(message = "Invoice must contain at least one line item")
    @Builder.Default
    private List<InvoiceLineDto> lines = new ArrayList<>();
}
