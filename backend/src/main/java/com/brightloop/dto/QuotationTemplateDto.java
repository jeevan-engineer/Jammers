package com.brightloop.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for creating, updating, and returning Quotation Templates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationTemplateDto {

    private Long id;

    @NotBlank(message = "Template name is required")
    private String name;

    private String description;

    @NotBlank(message = "Recurring interval is required (e.g., MONTHLY, YEARLY)")
    @Builder.Default
    private String recurringInterval = "MONTHLY";

    @NotNull(message = "Duration is required")
    @Builder.Default
    private Integer duration = 12;

    @NotBlank(message = "Duration unit is required")
    @Builder.Default
    private String durationUnit = "MONTHS";

    @Builder.Default
    private Boolean autoClose = false;

    @Builder.Default
    private Boolean active = true;

    private String termsAndConditions;

    private String notes;

    @Valid
    @Builder.Default
    private List<LineDto> lines = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineDto {
        private Long id;
        private Long productId;

        @NotBlank(message = "Product name is required")
        private String productName;

        private String description;

        @Builder.Default
        private BigDecimal quantity = BigDecimal.ONE;

        @Builder.Default
        private BigDecimal unitPrice = BigDecimal.ZERO;

        @Builder.Default
        private BigDecimal discount = BigDecimal.ZERO;

        @Builder.Default
        private BigDecimal subtotal = BigDecimal.ZERO;

        @Builder.Default
        private Integer sequence = 0;
    }
}
