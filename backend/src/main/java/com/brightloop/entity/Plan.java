package com.brightloop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_period", nullable = false, length = 20)
    private BillingPeriod billingPeriod;

    @Builder.Default
    @Column(name = "min_quantity", nullable = false)
    private Integer minQuantity = 1;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Column(name = "available_to")
    private LocalDate availableTo;

    @Builder.Default
    @Column(name = "auto_close", nullable = false)
    private Boolean autoClose = false;

    @Builder.Default
    @Column(name = "early_close", nullable = false)
    private Boolean earlyClose = true;

    @Builder.Default
    @Column(name = "pausable", nullable = false)
    private Boolean pausable = true;

    @Builder.Default
    @Column(name = "renewable", nullable = false)
    private Boolean renewable = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}