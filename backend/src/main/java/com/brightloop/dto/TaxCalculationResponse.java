package com.brightloop.dto;

import com.brightloop.enums.ProductType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TaxCalculationResponse {

    private Long taxId;

    private String taxName;

    private BigDecimal baseAmount;

    private BigDecimal percentage;

    private BigDecimal taxAmount;

    private BigDecimal totalAmount;

    private ProductType applicability;
}