package com.brightloop.dto;

import com.brightloop.enums.ProductType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TaxRequest {

    @NotBlank(message = "Tax name is required")
    @Size(max = 100)
    private String name;

    @NotNull(message = "Tax percentage is required")
    @DecimalMin(value = "0.00", message = "Tax percentage cannot be negative")
    @DecimalMax(value = "100.00", message = "Tax percentage cannot exceed 100")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal percentage;

    @NotNull(message = "Tax applicability is required")
    private ProductType applicability;

    private Boolean active = true;
}