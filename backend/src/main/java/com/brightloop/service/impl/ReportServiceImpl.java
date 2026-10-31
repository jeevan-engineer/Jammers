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
        log.info("Computing real-time dashboard analytics and financial reporting");

        long totalSubs = subscriptionRepository.count();
        long activeSubs = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
        long draftSubs = subscriptionRepository.countByStatus(SubscriptionStatus.DRAFT);
        long closedSubs = subscriptionRepository.countByStatus(SubscriptionStatus.CLOSED);

        BigDecimal mrr = subscriptionRepository.sumTotalAmountByStatus(SubscriptionStatus.ACTIVE);
        BigDecimal totalRevenue = invoiceRepository.sumTotalRevenue();

        List<InvoiceStatus> unsettled = List.of(InvoiceStatus.CONFIRMED, InvoiceStatus.UNPAID, InvoiceStatus.PARTIALLY_PAID);
        BigDecimal amountOwed = invoiceRepository.sumAmountOwed(unsettled);

        long totalInvoices = invoiceRepository.count();
        long paidInvoices = invoiceRepository.countByStatus(InvoiceStatus.PAID);
        long unpaidInvoices = invoiceRepository.countByStatus(InvoiceStatus.UNPAID);
        long overdueInvoices = invoiceRepository.countOverdue(LocalDate.now(), unsettled);

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
                .unpaidInvoices(unpaidInvoices)
                .overdueInvoices(overdueInvoices)
                .recentActivities(activityDtos)
                .build();
    }
}
