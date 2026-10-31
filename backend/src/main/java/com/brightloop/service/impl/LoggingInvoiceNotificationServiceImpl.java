package com.brightloop.service.impl;

import com.brightloop.entity.Invoice;
import com.brightloop.service.InvoiceNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Clean architectural boundary implementation for invoice notification dispatch.
 */
@Slf4j
@Service
public class LoggingInvoiceNotificationServiceImpl implements InvoiceNotificationService {

    @Override
    public void sendInvoiceNotification(Invoice invoice) {
        log.info("[NOTIFICATION DISPATCH] Dispatched invoice {} (Total: {} {}) to customer {} at {}",
                invoice.getInvoiceNumber(),
                invoice.getTotalAmount(),
                invoice.getCurrency(),
                invoice.getCustomerName(),
                invoice.getCustomerEmail() != null ? invoice.getCustomerEmail() : "billing-portal");
    }
}
