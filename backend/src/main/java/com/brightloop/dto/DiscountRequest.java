package com.brightloop.dto;

import com.brightloop.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Data
public class DiscountRequest {

    @NotBlank(message = "Discount code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    private String code;

    @NotBlank(message = "Discount name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String name;

    @NotNull(message = "Discount type is required (FIXED or PERCENTAGE)")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum purchase amount cannot be negative")
    private BigDecimal minPurchaseAmount = BigDecimal.ZERO;

    @Min(value = 1, message = "Minimum quantity must be at least 1")
    private Integer minQuantity = 1;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Min(value = 1, message = "Usage limit must be at least 1 if specified")
    private Integer usageLimit;

    private Set<Long> applicableProductIds;

    private Boolean active = true;
}