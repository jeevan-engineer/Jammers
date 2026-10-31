package com.brightloop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Metric summary payload for admin and management dashboard reporting.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardReportResponse {

    private long totalSubscriptions;
    private long activeSubscriptions;
    private long draftSubscriptions;
    private long closedSubscriptions;

    private BigDecimal totalRevenue;
    private BigDecimal amountOwed;
    private BigDecimal monthlyRecurringRevenue; // MRR

    private long totalInvoices;
    private long paidInvoices;
    private long partiallyPaidInvoices;
    private long unpaidInvoices;
    private long overdueInvoices;

    @Builder.Default
    private List<RecentActivityDto> recentActivities = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivityDto {
        private String eventType;
        private String referenceNumber;
        private String description;
        private BigDecimal amount;
        private String timestamp;
    }
}
