package com.brightloop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Line item defining specific recurring items, pricing, and quantities
 * within a QuotationTemplate.
 */
@Entity
@Table(name = "quotation_template_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "quotationTemplate")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class QuotationTemplateLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_template_id", nullable = false)
    private QuotationTemplate quotationTemplate;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity = BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);

    @Builder.Default
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Builder.Default
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Builder.Default
    @Column(nullable = false)
    private Integer sequence = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.quantity == null) {
            this.quantity = BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
        }
        if (this.unitPrice == null) {
            this.unitPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (this.discount == null) {
            this.discount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (this.sequence == null) {
            this.sequence = 0;
        }
        recalculateSubtotal();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        recalculateSubtotal();
    }

    public void recalculateSubtotal() {
        BigDecimal qty = this.quantity != null ? this.quantity : BigDecimal.ZERO;
        BigDecimal price = this.unitPrice != null ? this.unitPrice : BigDecimal.ZERO;
        BigDecimal gross = qty.multiply(price);

        if (this.discount != null && this.discount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                    this.discount.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
            );
            this.subtotal = gross.multiply(discountMultiplier).setScale(2, RoundingMode.HALF_UP);
        } else {
            this.subtotal = gross.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
