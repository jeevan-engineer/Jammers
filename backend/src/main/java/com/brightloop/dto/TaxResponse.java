package com.brightloop.dto;

import com.brightloop.enums.ProductType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TaxResponse {

    private Long id;

    private String name;

    private BigDecimal percentage;

    private ProductType applicability;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}