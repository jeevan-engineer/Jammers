package com.brightloop.dto;

import com.brightloop.entity.BillingPeriod;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PlanRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotBlank(message = "Plan name is required")
    @Size(max = 150, message = "Plan name must not exceed 150 characters")
    private String name;

    @NotNull(message = "Plan price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Plan price cannot be negative")
    @Digits(integer = 15, fraction = 4, message = "Invalid price format")
    private BigDecimal price;

    @NotNull(message = "Billing period is required (DAILY, WEEKLY, MONTHLY, YEARLY)")
    private BillingPeriod billingPeriod;

    @NotNull(message = "Minimum quantity is required")
    @Min(value = 1, message = "Minimum quantity must be at least 1")
    private Integer minQuantity = 1;

    private LocalDate availableFrom;

    private LocalDate availableTo;

    private Boolean autoClose = false;

    private Boolean earlyClose = true;

    private Boolean pausable = true;

    private Boolean renewable = true;

    private Boolean active = true;
}