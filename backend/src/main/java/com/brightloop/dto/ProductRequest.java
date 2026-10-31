package com.brightloop.dto;

import com.brightloop.entity.ProductType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Product name must not exceed 150 characters")
    private String name;

    @NotNull(message = "Product type is required (SERVICE, SAAS, CONSUMABLE, DIGITAL, LICENSE)")
    private ProductType type;

    @NotNull(message = "Sales price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Sales price cannot be negative")
    @Digits(integer = 15, fraction = 4, message = "Invalid sales price format")
    private BigDecimal salesPrice;

    @NotNull(message = "Cost price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Cost price cannot be negative")
    @Digits(integer = 15, fraction = 4, message = "Invalid cost price format")
    private BigDecimal costPrice;

    private String notes;

    private Boolean active = true;
}