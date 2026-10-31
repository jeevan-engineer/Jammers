package com.brightloop.subscription.feature.user.entity;

import com.brightloop.subscription.common.entity.BaseEntity;
import com.brightloop.subscription.common.enums.Role;
import com.brightloop.subscription.feature.customer.entity.Customer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * JPA entity mapping to the 'users' table.
 *
 * @Entity + @Table(name = "users") — registers this class as a JPA entity
 * mapped to the 'users' table. Hibernate validates column definitions
 * against the Flyway-created schema at startup (ddl-auto=validate).
 *
 * @Enumerated(EnumType.STRING) — stores the enum constant NAME ("ADMIN")
 * rather than its ordinal (0). This is resilient to enum reordering.
 *
 * 1:1 relationship with Customer:
 *   A User of role CUSTOMER MAY have one Customer profile.
 *   @OneToOne(mappedBy = "user") — Customer owns the FK (user_id column).
 *   CascadeType.ALL + orphanRemoval — if a User is deleted, the linked
 *   Customer row is also deleted (though the FK is SET NULL in SQL for
 *   staff-created customers; this cascade only applies to linked profiles).
 *   fetch = LAZY — avoids N+1 by not loading Customer unless explicitly called.
 *
 * Common error: forgetting updatable=false on column definitions can cause
 * Hibernate schema validation failures when the DDL has non-updatable defaults.
 */
@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Full display name. Maps to full_name VARCHAR(100) NOT NULL */
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /** Unique login email. Maps to email VARCHAR(150) NOT NULL UNIQUE */
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /** BCrypt-hashed password. Never expose this in any DTO. */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /** Role for RBAC. Stored as VARCHAR matching enum name. */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    /** Soft-disable flag. Inactive users cannot log in. */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Optional 1:1 link to the Customer profile.
     * Only set for users with role=CUSTOMER created through /api/auth/signup.
     * Staff users (ADMIN, INTERNAL) will have customerProfile = null.
     * mappedBy="user" — the FK column (user_id) lives on the customers table.
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL,
              orphanRemoval = true, fetch = FetchType.LAZY)
    private Customer customerProfile;
}
