-- =============================================================================
-- Migration: V4_2__add_invoice_issue_date_index.sql
-- Description: Performance optimization index on invoice issue_date for
--              high-volume financial dashboard filtering and reporting.
-- =============================================================================

CREATE INDEX idx_invoices_issue_date ON invoices (issue_date);
