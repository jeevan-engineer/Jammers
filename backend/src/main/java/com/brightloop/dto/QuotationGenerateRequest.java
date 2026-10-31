package com.brightloop.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request payload to generate a formal customer Quotation from a QuotationTemplate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationGenerateRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    private String customerName;
    private String customerEmail;
    private LocalDate startDate;
    private Integer validityDays;
    private String currency;
    private String notes;
}
