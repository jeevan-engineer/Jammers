package com.brightloop.subscription.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Base entity providing audit timestamps for all entities.
 *
 * @MappedSuperclass — JPA will include these fields in every subclass table
 * but will NOT create a separate table for BaseEntity itself.
 *
 * @EntityListeners(AuditingEntityListener.class) — hooks into Spring Data JPA
 * auditing (enabled via @EnableJpaAuditing on the main application class)
 * so that @CreatedDate and @LastModifiedDate are populated automatically.
 *
 * updatable=false on createdAt — once inserted, Hibernate never updates it.
 *
 * Common error: if @EnableJpaAuditing is missing from the application class,
 * createdAt/updatedAt will always be null at runtime.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /**
     * Set once at INSERT; matches DEFAULT CURRENT_TIMESTAMP in SQL.
     * nullable=false enforces the NOT NULL constraint Hibernate validates against.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Updated automatically by Spring Data JPA on every save/merge.
     * In MySQL this also aligns with ON UPDATE CURRENT_TIMESTAMP in the DDL.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
