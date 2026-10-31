package com.brightloop.dto;

import com.brightloop.entity.ProductType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private ProductType type;
    private BigDecimal salesPrice;
    private BigDecimal costPrice;
    private String notes;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}