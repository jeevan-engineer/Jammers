package com.brightloop.entity;

import com.brightloop.enums.SubscriptionStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Core recurring subscription entity representing a contract between BrightLoop
 * and a customer for periodic product/service access and automatic invoicing.
 */
@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"lines", "statusHistories", "invoices", "quotationTemplate", "paymentMethod"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "subscription_number", nullable = false, unique = true, length = 100)
    private String subscriptionNumber;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_email")
    private String customerEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_template_id")
    private QuotationTemplate quotationTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SubscriptionStatus status = SubscriptionStatus.DRAFT;

    @Builder.Default
    @Column(name = "billing_interval", nullable = false, length = 50)
    private String billingInterval = "MONTHLY";

    @Builder.Default
    @Column(name = "interval_count", nullable = false)
    private Integer intervalCount = 1;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "trial_start_date")
    private LocalDate trialStartDate;

    @Column(name = "trial_end_date")
    private LocalDate trialEndDate;

    @Column(name = "current_period_start", nullable = false)
    private LocalDate currentPeriodStart;

    @Column(name = "current_period_end", nullable = false)
    private LocalDate currentPeriodEnd;

    @Column(name = "next_billing_date")
    private LocalDate nextBillingDate;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "paused_at")
    private LocalDateTime pausedAt;

    @Column(name = "quote_valid_until")
    private LocalDate quoteValidUntil;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Builder.Default
    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Builder.Default
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Builder.Default
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Builder.Default
    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Builder.Default
    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew = true;

    @Column(columnDefinition = "TEXT")
    private String terms;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SubscriptionLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SubscriptionStatusHistory> statusHistories = new ArrayList<>();

    @OneToMany(mappedBy = "subscription")
    @Builder.Default
    private List<Invoice> invoices = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = SubscriptionStatus.DRAFT;
        }
        if (this.billingInterval == null) {
            this.billingInterval = "MONTHLY";
        }
        if (this.intervalCount == null) {
            this.intervalCount = 1;
        }
        if (this.startDate == null) {
            this.startDate = LocalDate.now();
        }
        if (this.currentPeriodStart == null) {
            this.currentPeriodStart = this.startDate;
        }
        if (this.currentPeriodEnd == null) {
            this.currentPeriodEnd = calculateInitialPeriodEnd(this.currentPeriodStart, this.billingInterval, this.intervalCount);
        }
        if (this.nextBillingDate == null) {
            this.nextBillingDate = this.currentPeriodEnd;
        }
        if (this.currency == null) {
            this.currency = "USD";
        }
        if (this.autoRenew == null) {
            this.autoRenew = true;
        }
        recalculateTotals();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        recalculateTotals();
    }

    private LocalDate calculateInitialPeriodEnd(LocalDate start, String interval, int count) {
        if (start == null) {
            return LocalDate.now();
        }
        return switch (interval != null ? interval.toUpperCase() : "MONTHLY") {
            case "YEARLY", "ANNUALLY" -> start.plusYears(count);
            case "QUARTERLY" -> start.plusMonths(count * 3L);
            case "WEEKLY" -> start.plusWeeks(count);
            case "DAILY" -> start.plusDays(count);
            default -> start.plusMonths(count);
        };
    }

    public void addLine(SubscriptionLine line) {
        if (line != null) {
            if (this.lines == null) {
                this.lines = new ArrayList<>();
            }
            this.lines.add(line);
            line.setSubscription(this);
            recalculateTotals();
        }
    }

    public void removeLine(SubscriptionLine line) {
        if (line != null && this.lines != null) {
            this.lines.remove(line);
            line.setSubscription(null);
            recalculateTotals();
        }
    }

    public void addStatusHistory(SubscriptionStatus newStatus, String changedBy, String reason, String comment) {
        SubscriptionStatus previous = this.status;
        this.status = newStatus;
        if (this.statusHistories == null) {
            this.statusHistories = new ArrayList<>();
        }
        SubscriptionStatusHistory history = SubscriptionStatusHistory.builder()
                .subscription(this)
                .previousStatus(previous)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .reason(reason)
                .comment(comment)
                .changedAt(LocalDateTime.now())
                .build();
        this.statusHistories.add(history);
    }

    public void recalculateTotals() {
        if (this.lines == null || this.lines.isEmpty()) {
            this.subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            this.taxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            this.discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            this.totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return;
        }

        BigDecimal calculatedSubtotal = BigDecimal.ZERO;
        BigDecimal calculatedTax = BigDecimal.ZERO;
        BigDecimal calculatedDiscount = BigDecimal.ZERO;

        for (SubscriptionLine line : this.lines) {
            if (line != null) {
                line.recalculateLineTotal();
                if (line.getSubtotal() != null) {
                    calculatedSubtotal = calculatedSubtotal.add(line.getSubtotal());
                }
                if (line.getTaxAmount() != null) {
                    calculatedTax = calculatedTax.add(line.getTaxAmount());
                }
                if (line.getDiscount() != null && line.getQuantity() != null && line.getUnitPrice() != null) {
                    BigDecimal lineGross = line.getQuantity().multiply(line.getUnitPrice());
                    BigDecimal lineDiscountVal = lineGross.multiply(line.getDiscount())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    calculatedDiscount = calculatedDiscount.add(lineDiscountVal);
                }
            }
        }

        this.subtotal = calculatedSubtotal.setScale(2, RoundingMode.HALF_UP);
        this.taxAmount = calculatedTax.setScale(2, RoundingMode.HALF_UP);
        this.discountAmount = calculatedDiscount.setScale(2, RoundingMode.HALF_UP);
        this.totalAmount = this.subtotal.add(this.taxAmount).setScale(2, RoundingMode.HALF_UP);
    }
}
