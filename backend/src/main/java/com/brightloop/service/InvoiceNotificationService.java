package com.brightloop.service;

import com.brightloop.entity.Invoice;

/**
 * Boundary interface for delivering invoices to customers via notification channels
 * (e.g. Email, SMS, Webhook, WhatsApp).
 */
public interface InvoiceNotificationService {
    void sendInvoiceNotification(Invoice invoice);
}
