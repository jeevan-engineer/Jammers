package com.brightloop.subscription.feature.customer.entity;

import com.brightloop.subscription.common.entity.BaseEntity;
import com.brightloop.subscription.feature.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * JPA entity mapping to the 'customers' table.
 *
 * 1:1 RELATIONSHIP with User (Customer owns the FK):
 *   @JoinColumn(name = "user_id") — tells Hibernate that the user_id
 *   column in THIS (customers) table is the FK to users.id.
 *   unique=true enforces the 1:1 constraint at the JPA level (matches the
 *   UNIQUE constraint on user_id in V1 SQL).
 *   nullable=true — user_id can be NULL for staff-created customers.
 *
 * @OneToOne(fetch = LAZY) — do NOT eagerly load User for every customer
 * query; only load when explicitly accessed.
 *
 * Common error: if the user_id column is defined as NOT NULL in JPA
 * but the SQL has it as NULL, ddl-auto=validate will reject the app at startup.
 */
@Entity
@Table(name = "customers")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Optional link to a user account (for customers who sign up via the portal).
     * NULL for customers created by staff without a login account.
     * UNIQUE ensures at most one Customer per User.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = true)
    private User user;

    /** Customer's display name. Maps to name VARCHAR(150) NOT NULL. */
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** Business contact email. Unique across all customers. */
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /** Optional contact phone number (7-15 digits). */
    @Column(name = "phone", nullable = true, length = 20)
    private String phone;

    /** Optional company / organisation name. */
    @Column(name = "company_name", nullable = true, length = 150)
    private String companyName;

    /** Optional billing address. */
    @Column(name = "address", nullable = true, length = 255)
    private String address;

    /** Optional tax/VAT identifier — used by Member 4 for invoicing. */
    @Column(name = "tax_id", nullable = true, length = 50)
    private String taxId;

    /** Soft-disable flag. Inactive customers are excluded from normal queries. */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;
}
