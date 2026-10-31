package com.brightloop.service.impl;

import com.brightloop.dto.InvoiceCreateRequest;
import com.brightloop.dto.InvoiceLineDto;
import com.brightloop.dto.InvoiceResponse;
import com.brightloop.dto.PaymentResponse;
import com.brightloop.entity.Invoice;
import com.brightloop.entity.InvoiceLine;
import com.brightloop.entity.Subscription;
import com.brightloop.enums.InvoiceStatus;
import com.brightloop.exception.BusinessValidationException;
import com.brightloop.exception.ResourceNotFoundException;
import com.brightloop.repository.InvoiceRepository;
import com.brightloop.repository.SubscriptionRepository;
import com.brightloop.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    public InvoiceResponse createInvoice(InvoiceCreateRequest request) {
        log.info("Creating invoice for customer ID: {}", request.getCustomerId());

        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new BusinessValidationException("Invoice must contain at least one line item");
        }

        Subscription subscription = null;
        if (request.getSubscriptionId() != null) {
            subscription = subscriptionRepository.findById(request.getSubscriptionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", request.getSubscriptionId()));

            // Strict Anti-Double Billing Constraint Enforcement
            boolean exists = invoiceRepository.existsBySubscriptionIdAndBillingPeriodStartAndBillingPeriodEnd(
                    request.getSubscriptionId(),
                    request.getBillingPeriodStart(),
                    request.getBillingPeriodEnd()
            );

            if (exists) {
                throw new BusinessValidationException(
                        String.format("An invoice already exists for subscription %s in billing period [%s to %s]",
                                subscription.getSubscriptionNumber(),
                                request.getBillingPeriodStart(),
                                request.getBillingPeriodEnd())
                );
            }
        }

        LocalDate issue = request.getIssueDate() != null ? request.getIssueDate() : LocalDate.now();
        LocalDate due = request.getDueDate() != null ? request.getDueDate() : issue.plusDays(30);
        String generatedNumber = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Invoice invoice = Invoice.builder()
                .invoiceNumber(generatedNumber)
                .subscription(subscription)
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .billingAddress(request.getBillingAddress())
                .billingPeriodStart(request.getBillingPeriodStart())
                .billingPeriodEnd(request.getBillingPeriodEnd())
                .status(request.getStatus() != null ? request.getStatus() : InvoiceStatus.DRAFT)
                .issueDate(issue)
                .dueDate(due)
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .notes(request.getNotes())
                .terms(request.getTerms())
                .lines(new ArrayList<>())
                .build();

        for (InvoiceLineDto lineDto : request.getLines()) {
            InvoiceLine line = InvoiceLine.builder()
                    .invoice(invoice)
                    .productId(lineDto.getProductId())
                    .productName(lineDto.getProductName())
                    .description(lineDto.getDescription())
                    .quantity(lineDto.getQuantity() != null ? lineDto.getQuantity() : BigDecimal.ONE)
                    .unitPrice(lineDto.getUnitPrice() != null ? lineDto.getUnitPrice() : BigDecimal.ZERO)
                    .discount(lineDto.getDiscount() != null ? lineDto.getDiscount() : BigDecimal.ZERO)
                    .taxRate(lineDto.getTaxRate() != null ? lineDto.getTaxRate() : BigDecimal.ZERO)
                    .build();
            invoice.addLine(line);
        }

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice created successfully with number: {}", saved.getInvoiceNumber());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));
        return mapToResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByNumber(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "invoiceNumber", invoiceNumber));
        return mapToResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getAllInvoices(String status, Long customerId, Pageable pageable) {
        if (status != null && !status.isBlank() && customerId != null) {
            InvoiceStatus invoiceStatus = InvoiceStatus.valueOf(status.toUpperCase());
            return invoiceRepository.findByStatusAndCustomerId(invoiceStatus, customerId, pageable)
                    .map(this::mapToResponse);
        } else if (status != null && !status.isBlank()) {
            InvoiceStatus invoiceStatus = InvoiceStatus.valueOf(status.toUpperCase());
            return invoiceRepository.findByStatus(invoiceStatus, pageable)
                    .map(this::mapToResponse);
        } else if (customerId != null) {
            return invoiceRepository.findByCustomerId(customerId, pageable)
                    .map(this::mapToResponse);
        } else {
            return invoiceRepository.findAll(pageable)
                    .map(this::mapToResponse);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoicesBySubscriptionId(Long subscriptionId) {
        return invoiceRepository.findBySubscriptionId(subscriptionId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public InvoiceResponse confirmInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessValidationException("Only invoices in DRAFT status can be confirmed");
        }

        invoice.setStatus(InvoiceStatus.CONFIRMED);
        Invoice updated = invoiceRepository.save(invoice);
        log.info("Invoice {} confirmed", updated.getInvoiceNumber());
        return mapToResponse(updated);
    }

    @Override
    public InvoiceResponse cancelInvoice(Long id, String reason) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", id));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessValidationException("Cannot cancel an already PAID invoice. Process a refund instead.");
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            invoice.setNotes((invoice.getNotes() != null ? invoice.getNotes() + " | Cancellation reason: " : "Cancellation reason: ") + reason);
        }

        Invoice updated = invoiceRepository.save(invoice);
        log.info("Invoice {} cancelled", updated.getInvoiceNumber());
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getOverdueInvoices() {
        List<InvoiceStatus> unsettled = List.of(InvoiceStatus.CONFIRMED, InvoiceStatus.UNPAID, InvoiceStatus.PARTIALLY_PAID);
        return invoiceRepository.findOverdue(LocalDate.now(), unsettled).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private InvoiceResponse mapToResponse(Invoice invoice) {
        List<InvoiceLineDto> lineDtos = invoice.getLines().stream()
                .map(line -> InvoiceLineDto.builder()
                        .id(line.getId())
                        .productId(line.getProductId())
                        .productName(line.getProductName())
                        .description(line.getDescription())
                        .quantity(line.getQuantity())
                        .unitPrice(line.getUnitPrice())
                        .discount(line.getDiscount())
                        .taxRate(line.getTaxRate())
                        .taxAmount(line.getTaxAmount())
                        .subtotal(line.getSubtotal())
                        .totalAmount(line.getTotalAmount())
                        .build())
                .collect(Collectors.toList());

        List<PaymentResponse> paymentDtos = invoice.getPayments().stream()
                .map(pmt -> PaymentResponse.builder()
                        .id(pmt.getId())
                        .paymentNumber(pmt.getPaymentNumber())
                        .invoiceId(invoice.getId())
                        .invoiceNumber(invoice.getInvoiceNumber())
                        .paymentMethodId(pmt.getPaymentMethod() != null ? pmt.getPaymentMethod().getId() : null)
                        .paymentMethodType(pmt.getPaymentMethod() != null ? pmt.getPaymentMethod().getType().name() : null)
                        .customerId(pmt.getCustomerId())
                        .amount(pmt.getAmount())
                        .currency(pmt.getCurrency())
                        .paymentDate(pmt.getPaymentDate())
                        .status(pmt.getStatus())
                        .transactionReference(pmt.getTransactionReference())
                        .gatewayResponse(pmt.getGatewayResponse())
                        .notes(pmt.getNotes())
                        .createdAt(pmt.getCreatedAt())
                        .updatedAt(pmt.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .subscriptionId(invoice.getSubscription() != null ? invoice.getSubscription().getId() : null)
                .subscriptionNumber(invoice.getSubscription() != null ? invoice.getSubscription().getSubscriptionNumber() : null)
                .customerId(invoice.getCustomerId())
                .customerName(invoice.getCustomerName())
                .customerEmail(invoice.getCustomerEmail())
                .billingAddress(invoice.getBillingAddress())
                .billingPeriodStart(invoice.getBillingPeriodStart())
                .billingPeriodEnd(invoice.getBillingPeriodEnd())
                .status(invoice.getStatus())
                .issueDate(invoice.getIssueDate())
                .dueDate(invoice.getDueDate())
                .paidAt(invoice.getPaidAt())
                .subtotal(invoice.getSubtotal())
                .taxAmount(invoice.getTaxAmount())
                .discountAmount(invoice.getDiscountAmount())
                .totalAmount(invoice.getTotalAmount())
                .amountPaid(invoice.getAmountPaid())
                .amountDue(invoice.getAmountDue())
                .currency(invoice.getCurrency())
                .notes(invoice.getNotes())
                .terms(invoice.getTerms())
                .lines(lineDtos)
                .payments(paymentDtos)
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }
}
