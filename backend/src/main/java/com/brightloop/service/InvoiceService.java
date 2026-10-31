package com.brightloop.service;

import com.brightloop.dto.InvoiceCreateRequest;
import com.brightloop.dto.InvoiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InvoiceService {

    InvoiceResponse createInvoice(InvoiceCreateRequest request);

    InvoiceResponse getInvoiceById(Long id);

    InvoiceResponse getInvoiceByNumber(String invoiceNumber);

    Page<InvoiceResponse> getAllInvoices(String status, Long customerId, Pageable pageable);

    List<InvoiceResponse> getInvoicesBySubscriptionId(Long subscriptionId);

    InvoiceResponse confirmInvoice(Long id);

    InvoiceResponse cancelInvoice(Long id, String reason);

    List<InvoiceResponse> getOverdueInvoices();
}
