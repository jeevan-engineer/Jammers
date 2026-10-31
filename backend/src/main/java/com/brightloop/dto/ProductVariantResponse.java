package com.brightloop.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductVariantResponse {

    private Long id;

    private Long productId;

    private String productName;

    private String attribute;

    private String value;

    private BigDecimal extraPrice;

    private BigDecimal effectivePrice;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}