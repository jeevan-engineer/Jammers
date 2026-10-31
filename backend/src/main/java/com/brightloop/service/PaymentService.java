package com.brightloop.service;

import com.brightloop.dto.PaymentRecordRequest;
import com.brightloop.dto.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PaymentService {

    PaymentResponse recordPayment(PaymentRecordRequest request);

    PaymentResponse getPaymentById(Long id);

    PaymentResponse getPaymentByNumber(String paymentNumber);

    List<PaymentResponse> getPaymentsByInvoiceId(Long invoiceId);

    Page<PaymentResponse> getPaymentsByCustomerId(Long customerId, Pageable pageable);
}
