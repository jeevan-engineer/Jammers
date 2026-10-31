
-- ============================================================
-- V1__create_users_and_customers.sql
-- Brightloop Subscription Management System
--
-- CONFIRM THIS VERSION NUMBER (V1) WITH MEMBER 1 BEFORE COMMITTING.
-- If Member 1 already has a V1 migration, rename this file to V2__...
-- Flyway uses the version number to determine migration order.
--
-- Engine : InnoDB (supports FK constraints and transactions)
-- Charset: utf8mb4 + utf8mb4_unicode_ci (full Unicode, including emoji)
-- ddl-auto is set to 'validate', so Hibernate will verify that the column
-- types, nullability, and names EXACTLY match the @Entity definitions.
-- ============================================================


-- -------------------------------------------------------
-- TABLE: users
-- Stores both staff (ADMIN, INTERNAL) and customer login accounts.
-- role is stored as VARCHAR matching the Java Role enum name.
-- -------------------------------------------------------

CREATE TABLE IF NOT EXISTS users (
                                     id            BIGINT          NOT NULL AUTO_INCREMENT,
                                     full_name     VARCHAR(100)    NOT NULL,
    email         VARCHAR(150)    NOT NULL,
    password_hash VARCHAR(255)    NOT NULL,
    role          VARCHAR(20)     NOT NULL COMMENT 'ADMIN | INTERNAL | CUSTOMER',
    active        BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci
    COMMENT='Stores all user accounts: admins, internal staff, and customer logins';


CREATE INDEX idx_users_role ON users (role);


-- -------------------------------------------------------
-- TABLE: customers
-- Business-facing customer profiles.
--
-- 1:1 RELATIONSHIP with users (optional on both sides):
--   A Customer row MAY link to one User row (user_id FK, UNIQUE).
--   A User with role=CUSTOMER has AT MOST one Customer row.
--   Staff/admin users have NO customer row.
--   user_id is NULL for customers created by staff without a login.
--   ON DELETE SET NULL ensures the customer profile is retained even
--   if the linked user account is deleted.
-- -------------------------------------------------------

CREATE TABLE IF NOT EXISTS customers (
                                         id           BIGINT          NOT NULL AUTO_INCREMENT,
                                         user_id      BIGINT          NULL COMMENT 'FK to users.id; NULL = no login account',
                                         name         VARCHAR(150)    NOT NULL,
    email        VARCHAR(150)    NOT NULL,
    phone        VARCHAR(20)     NULL,
    company_name VARCHAR(150)    NULL,
    address      VARCHAR(255)    NULL,
    tax_id       VARCHAR(50)     NULL,
    active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uq_customers_email UNIQUE (email),
    CONSTRAINT uq_customers_user_id UNIQUE (user_id),
    CONSTRAINT fk_customers_user_id
    FOREIGN KEY (user_id) REFERENCES users (id)
                                                                    ON DELETE SET NULL
                                                                    ON UPDATE CASCADE
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci
    COMMENT='Business customer profiles; optionally linked to a user account';


CREATE INDEX idx_customers_name ON customers (name);
CREATE INDEX idx_customers_email ON customers (email);
