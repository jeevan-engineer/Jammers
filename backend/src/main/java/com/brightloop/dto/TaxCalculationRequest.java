package com.brightloop.dto;

import com.brightloop.enums.ProductType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TaxCalculationRequest {

    @NotNull(message = "Base amount is required")
    @DecimalMin(value = "0.00", message = "Amount cannot be negative")
    @Digits(integer = 15, fraction = 4)
    private BigDecimal baseAmount;

    @NotNull(message = "Product type is required")
    private ProductType productType;
}