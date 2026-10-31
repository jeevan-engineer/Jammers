package com.brightloop.service;

import com.brightloop.dto.DashboardReportResponse;

public interface ReportService {

    DashboardReportResponse getDashboardReport();

    DashboardReportResponse getDashboardReport(java.time.LocalDate startDate, java.time.LocalDate endDate);

    org.springframework.data.domain.Page<com.brightloop.dto.PaymentResponse> getPaymentHistory(org.springframework.data.domain.Pageable pageable);
}
