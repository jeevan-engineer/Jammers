package com.brightloop.service.impl;

import com.brightloop.dto.PaymentRecordRequest;
import com.brightloop.dto.PaymentResponse;
import com.brightloop.entity.Invoice;
import com.brightloop.entity.Payment;
import com.brightloop.entity.PaymentMethod;
import com.brightloop.enums.InvoiceStatus;
import com.brightloop.exception.BusinessValidationException;
import com.brightloop.exception.ResourceNotFoundException;
import com.brightloop.repository.InvoiceRepository;
import com.brightloop.repository.PaymentMethodRepository;
import com.brightloop.repository.PaymentRepository;
import com.brightloop.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    @Override
    public PaymentResponse recordPayment(PaymentRecordRequest request) {
        log.info("Recording payment of {} for invoice ID: {}", request.getAmount(), request.getInvoiceId());

        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", request.getInvoiceId()));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessValidationException("Invoice " + invoice.getInvoiceNumber() + " is already paid in full.");
        }

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessValidationException("Cannot apply payment to CANCELLED invoice " + invoice.getInvoiceNumber());
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("Payment amount must be greater than zero");
        }

        if (request.getAmount().compareTo(invoice.getAmountDue()) > 0) {
            throw new BusinessValidationException(
                    String.format("Payment amount %s exceeds outstanding balance %s for invoice %s",
                            request.getAmount(), invoice.getAmountDue(), invoice.getInvoiceNumber())
            );
        }

        PaymentMethod paymentMethod = null;
        if (request.getPaymentMethodId() != null) {
            paymentMethod = paymentMethodRepository.findById(request.getPaymentMethodId())
                    .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", "id", request.getPaymentMethodId()));
        }

        String paymentNumber = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime payDate = request.getPaymentDate() != null ? request.getPaymentDate() : LocalDateTime.now();

        Payment payment = Payment.builder()
                .paymentNumber(paymentNumber)
                .invoice(invoice)
                .paymentMethod(paymentMethod)
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : invoice.getCurrency())
                .paymentDate(payDate)
                .status("SUCCESS")
                .transactionReference(request.getTransactionReference())
                .gatewayResponse(request.getGatewayResponse())
                .notes(request.getNotes())
                .build();

        // Update financial ledger state on the invoice
        invoice.recordPayment(request.getAmount());
        invoiceRepository.save(invoice);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment recorded successfully with number: {}", savedPayment.getPaymentNumber());
        return mapToResponse(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByNumber(String paymentNumber) {
        Payment payment = paymentRepository.findByPaymentNumber(paymentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paymentNumber", paymentNumber));
        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByInvoiceId(Long invoiceId) {
        return paymentRepository.findByInvoiceId(invoiceId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByCustomerId(Long customerId, Pageable pageable) {
        return paymentRepository.findByCustomerId(customerId, pageable)
                .map(this::mapToResponse);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentNumber(payment.getPaymentNumber())
                .invoiceId(payment.getInvoice().getId())
                .invoiceNumber(payment.getInvoice().getInvoiceNumber())
                .paymentMethodId(payment.getPaymentMethod() != null ? payment.getPaymentMethod().getId() : null)
                .paymentMethodType(payment.getPaymentMethod() != null ? payment.getPaymentMethod().getType().name() : null)
                .customerId(payment.getCustomerId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentDate(payment.getPaymentDate())
                .status(payment.getStatus())
                .transactionReference(payment.getTransactionReference())
                .gatewayResponse(payment.getGatewayResponse())
                .notes(payment.getNotes())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
