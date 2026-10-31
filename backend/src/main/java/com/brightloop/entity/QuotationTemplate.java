package com.brightloop.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * QuotationTemplate entity represents reusable subscription quotation configurations
 * used by sales teams to rapidly generate consistent customer proposals.
 */
@Entity
@Table(name = "quotation_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "lines")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class QuotationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "recurring_interval", nullable = false, length = 50)
    private String recurringInterval = "MONTHLY";

    @Builder.Default
    @Column(nullable = false)
    private Integer duration = 12;

    @Builder.Default
    @Column(name = "duration_unit", nullable = false, length = 50)
    private String durationUnit = "MONTHS";

    @Builder.Default
    @Column(name = "validity_days", nullable = false)
    private Integer validityDays = 30;

    @Builder.Default
    @Column(name = "auto_close", nullable = false)
    private Boolean autoClose = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "quotationTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QuotationTemplateLine> lines = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.recurringInterval == null) {
            this.recurringInterval = "MONTHLY";
        }
        if (this.duration == null) {
            this.duration = 12;
        }
        if (this.durationUnit == null) {
            this.durationUnit = "MONTHS";
        }
        if (this.validityDays == null) {
            this.validityDays = 30;
        }
        if (this.autoClose == null) {
            this.autoClose = false;
        }
        if (this.active == null) {
            this.active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addLine(QuotationTemplateLine line) {
        if (line != null) {
            if (this.lines == null) {
                this.lines = new ArrayList<>();
            }
            this.lines.add(line);
            line.setQuotationTemplate(this);
        }
    }

    public void removeLine(QuotationTemplateLine line) {
        if (line != null && this.lines != null) {
            this.lines.remove(line);
            line.setQuotationTemplate(null);
        }
    }
}
