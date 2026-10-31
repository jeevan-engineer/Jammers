package com.brightloop.service.impl;

import com.brightloop.dto.DashboardReportResponse;
import com.brightloop.entity.Payment;
import com.brightloop.enums.InvoiceStatus;
import com.brightloop.enums.SubscriptionStatus;
import com.brightloop.repository.InvoiceRepository;
import com.brightloop.repository.PaymentRepository;
import com.brightloop.repository.SubscriptionRepository;
import com.brightloop.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public DashboardReportResponse getDashboardReport() {
        return getDashboardReport(null, null);
    }

    @Override
    public DashboardReportResponse getDashboardReport(LocalDate startDate, LocalDate endDate) {
        log.info("Computing real-time dashboard analytics (startDate: {}, endDate: {})", startDate, endDate);

        long totalSubs = subscriptionRepository.count();
        long activeSubs = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
        long draftSubs = subscriptionRepository.countByStatus(SubscriptionStatus.DRAFT);
        long closedSubs = subscriptionRepository.countByStatus(SubscriptionStatus.CLOSED);

        BigDecimal mrr = subscriptionRepository.sumTotalAmountByStatus(SubscriptionStatus.ACTIVE);

        List<InvoiceStatus> unsettled = List.of(InvoiceStatus.CONFIRMED, InvoiceStatus.UNPAID, InvoiceStatus.PARTIALLY_PAID);

        BigDecimal totalRevenue;
        BigDecimal amountOwed;
        long totalInvoices;
        long paidInvoices;
        long partiallyPaidInvoices;
        long unpaidInvoices;
        long overdueInvoices;

        if (startDate != null && endDate != null) {
            totalRevenue = invoiceRepository.sumTotalRevenueBetween(startDate, endDate);
            amountOwed = invoiceRepository.sumAmountOwedBetween(unsettled, startDate, endDate);
            totalInvoices = invoiceRepository.countBetween(startDate, endDate);
            paidInvoices = invoiceRepository.countByStatusBetween(InvoiceStatus.PAID, startDate, endDate);
            partiallyPaidInvoices = invoiceRepository.countByStatusBetween(InvoiceStatus.PARTIALLY_PAID, startDate, endDate);
            unpaidInvoices = invoiceRepository.countByStatusBetween(InvoiceStatus.UNPAID, startDate, endDate);
            overdueInvoices = invoiceRepository.countOverdueBetween(LocalDate.now(), unsettled, startDate, endDate);
        } else {
            totalRevenue = invoiceRepository.sumTotalRevenue();
            amountOwed = invoiceRepository.sumAmountOwed(unsettled);
            totalInvoices = invoiceRepository.count();
            paidInvoices = invoiceRepository.countByStatus(InvoiceStatus.PAID);
            partiallyPaidInvoices = invoiceRepository.countByStatus(InvoiceStatus.PARTIALLY_PAID);
            unpaidInvoices = invoiceRepository.countByStatus(InvoiceStatus.UNPAID);
            overdueInvoices = invoiceRepository.countOverdue(LocalDate.now(), unsettled);
        }

        List<Payment> recentPayments = paymentRepository.findTop10ByOrderByPaymentDateDesc();
        List<DashboardReportResponse.RecentActivityDto> activityDtos = new ArrayList<>();

        for (Payment payment : recentPayments) {
            activityDtos.add(DashboardReportResponse.RecentActivityDto.builder()
                    .eventType("PAYMENT_RECEIVED")
                    .referenceNumber(payment.getPaymentNumber())
                    .description(String.format("Payment received for invoice %s", payment.getInvoice().getInvoiceNumber()))
                    .amount(payment.getAmount())
                    .timestamp(payment.getPaymentDate() != null ? payment.getPaymentDate().toString() : null)
                    .build());
        }

        return DashboardReportResponse.builder()
                .totalSubscriptions(totalSubs)
                .activeSubscriptions(activeSubs)
                .draftSubscriptions(draftSubs)
                .closedSubscriptions(closedSubs)
                .totalRevenue(totalRevenue)
                .amountOwed(amountOwed)
                .monthlyRecurringRevenue(mrr)
                .totalInvoices(totalInvoices)
                .paidInvoices(paidInvoices)
                .partiallyPaidInvoices(partiallyPaidInvoices)
                .unpaidInvoices(unpaidInvoices)
                .overdueInvoices(overdueInvoices)
                .recentActivities(activityDtos)
                .build();
    }

    @Override
    public org.springframework.data.domain.Page<com.brightloop.dto.PaymentResponse> getPaymentHistory(org.springframework.data.domain.Pageable pageable) {
        return paymentRepository.findAllByOrderByPaymentDateDesc(pageable)
                .map(payment -> com.brightloop.dto.PaymentResponse.builder()
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
                        .build());
    }
}
