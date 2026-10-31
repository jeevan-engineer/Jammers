package com.brightloop.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductVariantRequest {

    @NotBlank(message = "Attribute is required")
    @Size(max = 100)
    private String attribute;

    @NotBlank(message = "Variant value is required")
    @Size(max = 150)
    private String value;

    @NotNull(message = "Extra price is required")
    @DecimalMin(value = "0.0", message = "Extra price cannot be negative")
    @Digits(integer = 15, fraction = 4)
    private BigDecimal extraPrice;

    private Boolean active = true;
}