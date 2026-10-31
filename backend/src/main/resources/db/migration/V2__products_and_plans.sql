-- =============================================================================
-- Migration: V2__products_and_plans.sql
-- Description: Core schema for Member 3 - Products and Plans
-- =============================================================================

CREATE TABLE IF NOT EXISTS products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    type VARCHAR(50) NOT NULL,
    sales_price DECIMAL(19, 4) NOT NULL,
    cost_price DECIMAL(19, 4) NOT NULL,
    notes TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_products PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS plans (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    price DECIMAL(19, 4) NOT NULL,
    billing_period VARCHAR(20) NOT NULL,
    min_quantity INT NOT NULL DEFAULT 1,
    available_from DATE NULL,
    available_to DATE NULL,
    auto_close BOOLEAN NOT NULL DEFAULT FALSE,
    early_close BOOLEAN NOT NULL DEFAULT TRUE,
    pausable BOOLEAN NOT NULL DEFAULT TRUE,
    renewable BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_plans PRIMARY KEY (id),
    CONSTRAINT fk_plans_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_products_name ON products (name);
CREATE INDEX idx_plans_product_id ON plans (product_id);
