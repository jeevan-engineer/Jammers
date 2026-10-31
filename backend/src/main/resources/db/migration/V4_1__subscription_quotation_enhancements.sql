-- =============================================================================
-- Migration: V4_1__subscription_quotation_enhancements.sql
-- Description: Schema extensions for quotation validity, taxes, transaction indexing,
--              and performance optimization for financial dashboards.
-- =============================================================================

-- 1. Add validity_days to quotation_templates
ALTER TABLE quotation_templates 
    ADD COLUMN validity_days INT NOT NULL DEFAULT 30;

-- 2. Add tax_rate to quotation_template_lines
ALTER TABLE quotation_template_lines 
    ADD COLUMN tax_rate DECIMAL(12, 2) NOT NULL DEFAULT 0.00;

-- 3. Add quote_valid_until to subscriptions
ALTER TABLE subscriptions 
    ADD COLUMN quote_valid_until DATE NULL;

-- 4. Add indexes for payment transaction references and created_at timestamps for dashboard performance
CREATE INDEX idx_payments_txn_ref ON payments (transaction_reference);
CREATE INDEX idx_invoices_created_at ON invoices (created_at);
CREATE INDEX idx_payments_created_at ON payments (created_at);
CREATE INDEX idx_sub_paused_at ON subscriptions (paused_at);
