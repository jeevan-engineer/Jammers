-- =============================================================================
-- Migration: V3__taxes_discounts_product_variants.sql
-- Description: Schema for Taxes, Discounts, Discount Usages, and Product Variants
-- Depends on: V2__products_and_plans.sql (products table)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. TABLE: taxes
-- Tax rates applicable to products/plans
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS taxes (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    name         VARCHAR(100)    NOT NULL,
    percentage   DECIMAL(5, 2)   NOT NULL,
    applicability VARCHAR(50)    NOT NULL COMMENT 'ProductType: SERVICE | CONSUMABLE | STORABLE',
    active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_taxes PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_taxes_active ON taxes (active);
CREATE INDEX idx_taxes_applicability ON taxes (applicability);

-- -----------------------------------------------------------------------------
-- 2. TABLE: discounts
-- Promotional discount codes
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS discounts (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    code                VARCHAR(50)     NOT NULL,
    name                VARCHAR(150)    NOT NULL,
    discount_type       VARCHAR(20)     NOT NULL COMMENT 'DiscountType: PERCENTAGE | FIXED_AMOUNT',
    discount_value      DECIMAL(19, 4)  NOT NULL,
    min_purchase_amount DECIMAL(19, 4)  NOT NULL DEFAULT 0.0000,
    min_quantity        INT             NOT NULL DEFAULT 1,
    start_date          DATE            NOT NULL,
    end_date            DATE            NOT NULL,
    usage_limit         INT             NULL,
    times_used          INT             NOT NULL DEFAULT 0,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_discounts PRIMARY KEY (id),
    CONSTRAINT uq_discounts_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_discounts_active ON discounts (active);
CREATE INDEX idx_discounts_code ON discounts (code);

-- -----------------------------------------------------------------------------
-- 3. TABLE: discount_products
-- Many-to-many join table: discounts <-> products
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS discount_products (
    discount_id BIGINT NOT NULL,
    product_id  BIGINT NOT NULL,
    CONSTRAINT pk_discount_products PRIMARY KEY (discount_id, product_id),
    CONSTRAINT fk_dp_discount FOREIGN KEY (discount_id) REFERENCES discounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_dp_product  FOREIGN KEY (product_id)  REFERENCES products (id)  ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 4. TABLE: discount_usages
-- Records each time a discount code is used
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS discount_usages (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    discount_id     BIGINT      NOT NULL,
    customer_id     BIGINT      NULL,
    subscription_id BIGINT      NULL,
    used_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_discount_usages PRIMARY KEY (id),
    CONSTRAINT fk_du_discount FOREIGN KEY (discount_id) REFERENCES discounts (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_du_discount_id ON discount_usages (discount_id);
CREATE INDEX idx_du_customer_id ON discount_usages (customer_id);

-- -----------------------------------------------------------------------------
-- 5. TABLE: product_variants
-- Product attribute variants (e.g. color: red, size: large)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_variants (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    product_id  BIGINT          NOT NULL,
    attribute   VARCHAR(100)    NOT NULL,
    value       VARCHAR(150)    NOT NULL,
    extra_price DECIMAL(19, 4)  NOT NULL DEFAULT 0.0000,
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_product_variants PRIMARY KEY (id),
    CONSTRAINT fk_pv_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pv_product_id ON product_variants (product_id);
CREATE INDEX idx_pv_active ON product_variants (active);

