package com.brightloop.controller;

import com.brightloop.dto.DashboardReportResponse;
import com.brightloop.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardReportResponse> getDashboardReport(
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate startDate,
            @org.springframework.web.bind.annotation.RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate endDate
    ) {
        return ResponseEntity.ok(reportService.getDashboardReport(startDate, endDate));
    }

    @GetMapping("/payments")
    public ResponseEntity<org.springframework.data.domain.Page<com.brightloop.dto.PaymentResponse>> getPaymentHistory(
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable
    ) {
        return ResponseEntity.ok(reportService.getPaymentHistory(pageable));
    }
}
