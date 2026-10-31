-- =============================================================================
-- Migration: V4__subscription_billing.sql
-- Description: Core schema for Member 4 - Subscriptions, Quotation Templates,
--              Invoices, Payments, and Payment Methods
-- Specification: Hackathon 2026 Aligned Schema
-- Database: MySQL 8.0+
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Table: quotation_templates
-- Pre-configured templates used by sales reps to generate subscription quotes
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS quotation_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    recurring_interval VARCHAR(50) NOT NULL DEFAULT 'MONTHLY',
    duration INT NOT NULL DEFAULT 12,
    duration_unit VARCHAR(50) NOT NULL DEFAULT 'MONTHS',
    auto_close BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    terms_and_conditions TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_quotation_templates_name (name),
    INDEX idx_quotation_templates_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 2. Table: quotation_template_lines
-- Line items associated with a quotation template
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS quotation_template_lines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quotation_template_id BIGINT NOT NULL,
    product_id BIGINT,
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    quantity DECIMAL(12, 2) NOT NULL DEFAULT 1.00,
    unit_price DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    discount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    subtotal DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    sequence INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_qtl_quotation_template FOREIGN KEY (quotation_template_id) 
        REFERENCES quotation_templates (id) ON DELETE CASCADE,
    INDEX idx_qtl_template_id (quotation_template_id),
    INDEX idx_qtl_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3. Table: payment_methods
-- Vaulted customer payment methods (CASH, CARD, BANK_TRANSFER, UPI)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payment_methods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    provider VARCHAR(100) NOT NULL DEFAULT 'MANUAL',
    provider_payment_method_id VARCHAR(255),
    card_last_four VARCHAR(4),
    card_brand VARCHAR(50),
    card_expiry_month INT,
    card_expiry_year INT,
    account_holder_name VARCHAR(255),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pm_customer_id (customer_id),
    INDEX idx_pm_type (type),
    INDEX idx_pm_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 4. Table: subscriptions
-- Central recurring subscription contracts
-- Status: DRAFT, QUOTATION, CONFIRMED, ACTIVE, CLOSED
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscription_number VARCHAR(100) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    customer_name VARCHAR(255),
    customer_email VARCHAR(255),
    quotation_template_id BIGINT,
    payment_method_id BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    billing_interval VARCHAR(50) NOT NULL DEFAULT 'MONTHLY',
    interval_count INT NOT NULL DEFAULT 1,
    start_date DATE NOT NULL,
    end_date DATE,
    trial_start_date DATE,
    trial_end_date DATE,
    current_period_start DATE NOT NULL,
    current_period_end DATE NOT NULL,
    next_billing_date DATE,
    cancelled_at TIMESTAMP NULL,
    cancellation_reason VARCHAR(500),
    paused_at TIMESTAMP NULL,
    subtotal DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    auto_renew BOOLEAN NOT NULL DEFAULT TRUE,
    terms TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_subscriptions_template FOREIGN KEY (quotation_template_id)
        REFERENCES quotation_templates (id) ON DELETE SET NULL,
    CONSTRAINT fk_subscriptions_payment_method FOREIGN KEY (payment_method_id)
        REFERENCES payment_methods (id) ON DELETE SET NULL,
    INDEX idx_sub_customer_id (customer_id),
    INDEX idx_sub_status (status),
    INDEX idx_sub_next_billing_date (next_billing_date),
    INDEX idx_sub_number (subscription_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 5. Table: subscription_lines
-- Itemized recurring line items for a subscription
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS subscription_lines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    product_id BIGINT,
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    quantity DECIMAL(12, 2) NOT NULL DEFAULT 1.00,
    unit_price DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    discount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    tax_rate DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    subtotal DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sub_lines_subscription FOREIGN KEY (subscription_id)
        REFERENCES subscriptions (id) ON DELETE CASCADE,
    INDEX idx_sub_lines_sub_id (subscription_id),
    INDEX idx_sub_lines_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 6. Table: subscription_status_history
-- Audit trail of subscription lifecycle status transitions
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS subscription_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    previous_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by VARCHAR(255),
    reason VARCHAR(500),
    comment TEXT,
    CONSTRAINT fk_ssh_subscription FOREIGN KEY (subscription_id)
        REFERENCES subscriptions (id) ON DELETE CASCADE,
    INDEX idx_ssh_sub_id (subscription_id),
    INDEX idx_ssh_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 7. Table: invoices
-- Invoices generated per billing cycle with strict anti-double-billing constraint
-- Status: DRAFT, CONFIRMED, UNPAID, PARTIALLY_PAID, PAID, CANCELLED
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_number VARCHAR(100) NOT NULL UNIQUE,
    subscription_id BIGINT NULL,
    customer_id BIGINT NOT NULL,
    customer_name VARCHAR(255),
    customer_email VARCHAR(255),
    billing_address TEXT,
    billing_period_start DATE NOT NULL,
    billing_period_end DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    paid_at TIMESTAMP NULL,
    subtotal DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    amount_paid DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    amount_due DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    notes TEXT,
    terms TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoices_subscription FOREIGN KEY (subscription_id)
        REFERENCES subscriptions (id) ON DELETE SET NULL,
    CONSTRAINT uq_invoice_subscription_period UNIQUE (subscription_id, billing_period_start, billing_period_end),
    INDEX idx_inv_customer_id (customer_id),
    INDEX idx_inv_subscription_id (subscription_id),
    INDEX idx_inv_status (status),
    INDEX idx_inv_due_date (due_date),
    INDEX idx_inv_number (invoice_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 8. Table: invoice_lines
-- Detailed line items on an invoice
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS invoice_lines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    product_id BIGINT,
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    quantity DECIMAL(12, 2) NOT NULL DEFAULT 1.00,
    unit_price DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    discount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    tax_rate DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    subtotal DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_inv_lines_invoice FOREIGN KEY (invoice_id)
        REFERENCES invoices (id) ON DELETE CASCADE,
    INDEX idx_inv_lines_invoice_id (invoice_id),
    INDEX idx_inv_lines_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 9. Table: payments
-- Payment transaction records against invoices
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_number VARCHAR(100) NOT NULL UNIQUE,
    invoice_id BIGINT NOT NULL,
    payment_method_id BIGINT NULL,
    customer_id BIGINT NOT NULL,
    amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    transaction_reference VARCHAR(255),
    gateway_response TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_invoice FOREIGN KEY (invoice_id)
        REFERENCES invoices (id) ON DELETE RESTRICT,
    CONSTRAINT fk_payments_method FOREIGN KEY (payment_method_id)
        REFERENCES payment_methods (id) ON DELETE SET NULL,
    INDEX idx_payments_invoice_id (invoice_id),
    INDEX idx_payments_customer_id (customer_id),
    INDEX idx_payments_payment_number (payment_number),
    INDEX idx_payments_status (status),
    INDEX idx_payments_payment_date (payment_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
