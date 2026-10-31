package com.brightloop.entity;

import com.brightloop.enums.InvoiceStatus;
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
import jakarta.persistence.UniqueConstraint;
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
 * Invoice entity representing a financial claim against a customer,
 * generated on schedule by a subscription or created as an ad-hoc billing event.
 * Enforces unique billing periods per subscription to prevent double-billing.
 */
@Entity
@Table(
    name = "invoices",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_invoice_subscription_period",
            columnNames = {"subscription_id", "billing_period_start", "billing_period_end"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"lines", "payments", "subscription"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 100)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "billing_address", columnDefinition = "TEXT")
    private String billingAddress;

    @Column(name = "billing_period_start", nullable = false)
    private LocalDate billingPeriodStart;

    @Column(name = "billing_period_end", nullable = false)
    private LocalDate billingPeriodEnd;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

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
    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Builder.Default
    @Column(name = "amount_due", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountDue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Builder.Default
    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String terms;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "invoice")
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

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
            this.status = InvoiceStatus.DRAFT;
        }
        if (this.issueDate == null) {
            this.issueDate = LocalDate.now();
        }
        if (this.dueDate == null) {
            this.dueDate = this.issueDate.plusDays(30);
        }
        if (this.billingPeriodStart == null) {
            this.billingPeriodStart = this.issueDate;
        }
        if (this.billingPeriodEnd == null) {
            this.billingPeriodEnd = this.dueDate;
        }
        if (this.currency == null) {
            this.currency = "USD";
        }
        recalculateTotals();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        recalculateTotals();
    }

    public void addLine(InvoiceLine line) {
        if (line != null) {
            if (this.lines == null) {
                this.lines = new ArrayList<>();
            }
            this.lines.add(line);
            line.setInvoice(this);
            recalculateTotals();
        }
    }

    public void removeLine(InvoiceLine line) {
        if (line != null && this.lines != null) {
            this.lines.remove(line);
            line.setInvoice(null);
            recalculateTotals();
        }
    }

    public void recordPayment(BigDecimal paidAmount) {
        if (paidAmount == null || paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (this.amountPaid == null) {
            this.amountPaid = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        this.amountPaid = this.amountPaid.add(paidAmount).setScale(2, RoundingMode.HALF_UP);
        this.amountDue = this.totalAmount.subtract(this.amountPaid).setScale(2, RoundingMode.HALF_UP);

        if (this.amountDue.compareTo(BigDecimal.ZERO) <= 0) {
            this.amountDue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            this.status = InvoiceStatus.PAID;
            this.paidAt = LocalDateTime.now();
        } else {
            this.status = InvoiceStatus.PARTIALLY_PAID;
        }
    }

    public void recalculateTotals() {
        if (this.lines == null || this.lines.isEmpty()) {
            this.subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            this.taxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            this.discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            this.totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            this.amountDue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return;
        }

        BigDecimal calculatedSubtotal = BigDecimal.ZERO;
        BigDecimal calculatedTax = BigDecimal.ZERO;
        BigDecimal calculatedDiscount = BigDecimal.ZERO;

        for (InvoiceLine line : this.lines) {
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

        if (this.amountPaid == null) {
            this.amountPaid = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        this.amountDue = this.totalAmount.subtract(this.amountPaid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
