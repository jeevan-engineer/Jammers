package com.brightloop.dto;

import com.brightloop.entity.BillingPeriod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PlanResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String name;
    private BigDecimal price;
    private BillingPeriod billingPeriod;
    private Integer minQuantity;
    private LocalDate availableFrom;
    private LocalDate availableTo;
    private Boolean autoClose;
    private Boolean earlyClose;
    private Boolean pausable;
    private Boolean renewable;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}