package com.brightloop.service.impl;

import com.brightloop.dto.SubscriptionCreateRequest;
import com.brightloop.dto.SubscriptionLineDto;
import com.brightloop.dto.SubscriptionResponse;
import com.brightloop.dto.SubscriptionStatusUpdateRequest;
import com.brightloop.entity.Invoice;
import com.brightloop.entity.InvoiceLine;
import com.brightloop.entity.PaymentMethod;
import com.brightloop.entity.QuotationTemplate;
import com.brightloop.entity.Subscription;
import com.brightloop.entity.SubscriptionLine;
import com.brightloop.entity.SubscriptionStatusHistory;
import com.brightloop.enums.InvoiceStatus;
import com.brightloop.enums.SubscriptionStatus;
import com.brightloop.exception.BusinessValidationException;
import com.brightloop.exception.ResourceNotFoundException;
import com.brightloop.repository.InvoiceRepository;
import com.brightloop.repository.PaymentMethodRepository;
import com.brightloop.repository.QuotationTemplateRepository;
import com.brightloop.repository.SubscriptionRepository;
import com.brightloop.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final QuotationTemplateRepository quotationTemplateRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final InvoiceRepository invoiceRepository;

    @Override
    public SubscriptionResponse createSubscription(SubscriptionCreateRequest request) {
        log.info("Creating new subscription for customer ID: {}", request.getCustomerId());

        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new BusinessValidationException("Subscription must contain at least one line item");
        }

        QuotationTemplate template = null;
        if (request.getQuotationTemplateId() != null) {
            template = quotationTemplateRepository.findById(request.getQuotationTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("QuotationTemplate", "id", request.getQuotationTemplateId()));
        }

        PaymentMethod paymentMethod = null;
        if (request.getPaymentMethodId() != null) {
            paymentMethod = paymentMethodRepository.findById(request.getPaymentMethodId())
                    .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", "id", request.getPaymentMethodId()));
        }

        LocalDate start = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();
        String interval = request.getBillingInterval() != null ? request.getBillingInterval() : "MONTHLY";
        int intervalCount = request.getIntervalCount() != null ? request.getIntervalCount() : 1;
        LocalDate periodEnd = calculatePeriodEnd(start, interval, intervalCount);

        String generatedSubNumber = "SUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Subscription subscription = Subscription.builder()
                .subscriptionNumber(generatedSubNumber)
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .quotationTemplate(template)
                .paymentMethod(paymentMethod)
                .status(request.getStatus() != null ? request.getStatus() : SubscriptionStatus.DRAFT)
                .billingInterval(interval)
                .intervalCount(intervalCount)
                .startDate(start)
                .endDate(request.getEndDate())
                .trialStartDate(request.getTrialStartDate())
                .trialEndDate(request.getTrialEndDate())
                .currentPeriodStart(start)
                .currentPeriodEnd(periodEnd)
                .nextBillingDate(periodEnd)
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .autoRenew(request.getAutoRenew() != null ? request.getAutoRenew() : true)
                .terms(request.getTerms())
                .notes(request.getNotes())
                .lines(new ArrayList<>())
                .statusHistories(new ArrayList<>())
                .build();

        for (SubscriptionLineDto lineDto : request.getLines()) {
            SubscriptionLine line = SubscriptionLine.builder()
                    .subscription(subscription)
                    .productId(lineDto.getProductId())
                    .productName(lineDto.getProductName())
                    .description(lineDto.getDescription())
                    .quantity(lineDto.getQuantity() != null ? lineDto.getQuantity() : BigDecimal.ONE)
                    .unitPrice(lineDto.getUnitPrice() != null ? lineDto.getUnitPrice() : BigDecimal.ZERO)
                    .discount(lineDto.getDiscount() != null ? lineDto.getDiscount() : BigDecimal.ZERO)
                    .taxRate(lineDto.getTaxRate() != null ? lineDto.getTaxRate() : BigDecimal.ZERO)
                    .build();
            subscription.addLine(line);
        }

        subscription.addStatusHistory(
                subscription.getStatus(),
                "SYSTEM",
                "Initial creation",
                "Subscription initialized in " + subscription.getStatus() + " state"
        );

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription created successfully with number: {}", saved.getSubscriptionNumber());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscriptionById(Long id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", id));
        return mapToResponse(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscriptionByNumber(String subscriptionNumber) {
        Subscription subscription = subscriptionRepository.findBySubscriptionNumber(subscriptionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "subscriptionNumber", subscriptionNumber));
        return mapToResponse(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> getAllSubscriptions(String status, Long customerId, Pageable pageable) {
        if (status != null && !status.isBlank() && customerId != null) {
            SubscriptionStatus subscriptionStatus = SubscriptionStatus.valueOf(status.toUpperCase());
            return subscriptionRepository.findByStatusAndCustomerId(subscriptionStatus, customerId, pageable)
                    .map(this::mapToResponse);
        } else if (status != null && !status.isBlank()) {
            SubscriptionStatus subscriptionStatus = SubscriptionStatus.valueOf(status.toUpperCase());
            return subscriptionRepository.findByStatus(subscriptionStatus, pageable)
                    .map(this::mapToResponse);
        } else if (customerId != null) {
            return subscriptionRepository.findByCustomerId(customerId, pageable)
                    .map(this::mapToResponse);
        } else {
            return subscriptionRepository.findAll(pageable)
                    .map(this::mapToResponse);
        }
    }

    @Override
    public SubscriptionResponse updateStatus(Long id, SubscriptionStatusUpdateRequest request) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", id));

        SubscriptionStatus current = subscription.getStatus();
        SubscriptionStatus target = request.getStatus();

        if (current == target) {
            return mapToResponse(subscription);
        }

        if (!current.canTransitionTo(target)) {
            throw new BusinessValidationException(
                    String.format("Cannot transition subscription %s from state %s to %s",
                            subscription.getSubscriptionNumber(), current, target)
            );
        }

        subscription.addStatusHistory(
                target,
                request.getChangedBy() != null ? request.getChangedBy() : "USER",
                request.getReason(),
                request.getComment()
        );

        if (target == SubscriptionStatus.CLOSED) {
            subscription.setCancelledAt(LocalDateTime.now());
            subscription.setCancellationReason(request.getReason());
        }

        Subscription updated = subscriptionRepository.save(subscription);
        log.info("Subscription {} status updated: {} -> {}", updated.getSubscriptionNumber(), current, target);
        return mapToResponse(updated);
    }

    @Override
    public SubscriptionResponse closeSubscription(Long id, String reason) {
        return updateStatus(id, SubscriptionStatusUpdateRequest.builder()
                .status(SubscriptionStatus.CLOSED)
                .reason(reason)
                .comment("Subscription closed by user or administrative action")
                .build());
    }

    @Override
    public List<Long> generateBillingInvoices() {
        LocalDate today = LocalDate.now();
        log.info("Running automatic subscription billing generator for date: {}", today);

        List<Subscription> dueSubscriptions = subscriptionRepository.findDueForBilling(SubscriptionStatus.ACTIVE, today);
        List<Long> generatedInvoiceIds = new ArrayList<>();

        for (Subscription sub : dueSubscriptions) {
            LocalDate periodStart = sub.getCurrentPeriodStart();
            LocalDate periodEnd = sub.getCurrentPeriodEnd();

            // Strict Anti-Double Billing Constraint Check
            boolean alreadyBilled = invoiceRepository.existsBySubscriptionIdAndBillingPeriodStartAndBillingPeriodEnd(
                    sub.getId(), periodStart, periodEnd
            );

            if (alreadyBilled) {
                log.warn("Skipping billing for subscription {}: Invoice already exists for period [{} - {}]",
                        sub.getSubscriptionNumber(), periodStart, periodEnd);
                continue;
            }

            String invoiceNumber = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            Invoice invoice = Invoice.builder()
                    .invoiceNumber(invoiceNumber)
                    .subscription(sub)
                    .customerId(sub.getCustomerId())
                    .customerName(sub.getCustomerName())
                    .customerEmail(sub.getCustomerEmail())
                    .billingPeriodStart(periodStart)
                    .billingPeriodEnd(periodEnd)
                    .status(InvoiceStatus.UNPAID)
                    .issueDate(today)
                    .dueDate(today.plusDays(15))
                    .currency(sub.getCurrency())
                    .notes("Automated recurring billing cycle for period " + periodStart + " to " + periodEnd)
                    .lines(new ArrayList<>())
                    .build();

            for (SubscriptionLine subLine : sub.getLines()) {
                InvoiceLine invLine = InvoiceLine.builder()
                        .invoice(invoice)
                        .productId(subLine.getProductId())
                        .productName(subLine.getProductName())
                        .description(subLine.getDescription())
                        .quantity(subLine.getQuantity())
                        .unitPrice(subLine.getUnitPrice())
                        .discount(subLine.getDiscount())
                        .taxRate(subLine.getTaxRate())
                        .build();
                invoice.addLine(invLine);
            }

            Invoice savedInvoice = invoiceRepository.save(invoice);
            generatedInvoiceIds.add(savedInvoice.getId());

            // Advance subscription billing periods
            LocalDate nextStart = periodEnd;
            LocalDate nextEnd = calculatePeriodEnd(nextStart, sub.getBillingInterval(), sub.getIntervalCount());
            sub.setCurrentPeriodStart(nextStart);
            sub.setCurrentPeriodEnd(nextEnd);
            sub.setNextBillingDate(nextEnd);
            subscriptionRepository.save(sub);

            log.info("Generated invoice {} for subscription {}", invoiceNumber, sub.getSubscriptionNumber());
        }

        return generatedInvoiceIds;
    }

    private LocalDate calculatePeriodEnd(LocalDate start, String interval, int count) {
        if (start == null) {
            return LocalDate.now().plusMonths(1);
        }
        return switch (interval != null ? interval.toUpperCase() : "MONTHLY") {
            case "YEARLY", "ANNUALLY" -> start.plusYears(count);
            case "QUARTERLY" -> start.plusMonths(count * 3L);
            case "WEEKLY" -> start.plusWeeks(count);
            case "DAILY" -> start.plusDays(count);
            default -> start.plusMonths(count);
        };
    }

    private SubscriptionResponse mapToResponse(Subscription subscription) {
        List<SubscriptionLineDto> lineDtos = subscription.getLines().stream()
                .map(line -> SubscriptionLineDto.builder()
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

        List<SubscriptionResponse.StatusHistoryDto> historyDtos = subscription.getStatusHistories().stream()
                .map(history -> SubscriptionResponse.StatusHistoryDto.builder()
                        .id(history.getId())
                        .previousStatus(history.getPreviousStatus())
                        .newStatus(history.getNewStatus())
                        .changedAt(history.getChangedAt())
                        .changedBy(history.getChangedBy())
                        .reason(history.getReason())
                        .comment(history.getComment())
                        .build())
                .collect(Collectors.toList());

        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .subscriptionNumber(subscription.getSubscriptionNumber())
                .customerId(subscription.getCustomerId())
                .customerName(subscription.getCustomerName())
                .customerEmail(subscription.getCustomerEmail())
                .quotationTemplateId(subscription.getQuotationTemplate() != null ? subscription.getQuotationTemplate().getId() : null)
                .quotationTemplateName(subscription.getQuotationTemplate() != null ? subscription.getQuotationTemplate().getName() : null)
                .paymentMethodId(subscription.getPaymentMethod() != null ? subscription.getPaymentMethod().getId() : null)
                .status(subscription.getStatus())
                .billingInterval(subscription.getBillingInterval())
                .intervalCount(subscription.getIntervalCount())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .trialStartDate(subscription.getTrialStartDate())
                .trialEndDate(subscription.getTrialEndDate())
                .currentPeriodStart(subscription.getCurrentPeriodStart())
                .currentPeriodEnd(subscription.getCurrentPeriodEnd())
                .nextBillingDate(subscription.getNextBillingDate())
                .cancelledAt(subscription.getCancelledAt())
                .cancellationReason(subscription.getCancellationReason())
                .pausedAt(subscription.getPausedAt())
                .subtotal(subscription.getSubtotal())
                .taxAmount(subscription.getTaxAmount())
                .discountAmount(subscription.getDiscountAmount())
                .totalAmount(subscription.getTotalAmount())
                .currency(subscription.getCurrency())
                .autoRenew(subscription.getAutoRenew())
                .terms(subscription.getTerms())
                .notes(subscription.getNotes())
                .lines(lineDtos)
                .statusHistories(historyDtos)
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }
}
