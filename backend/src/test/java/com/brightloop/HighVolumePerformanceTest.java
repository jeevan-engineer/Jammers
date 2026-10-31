package com.brightloop;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class HighVolumePerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(HighVolumePerformanceTest.class);

    private static final long BENCH_CUSTOMER_START = 880000L;
    private static final int SUB_COUNT = 5000;
    private static final int INV_COUNT = 10000;
    private static final int PMT_COUNT = 20000;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Phase 8: High-Volume Performance Benchmark (5k Subscriptions, 10k Invoices, 20k Payments)")
    void runHighVolumeBenchmark() throws Exception {
        log.info("================================================================================");
        log.info("STARTING HIGH-VOLUME BENCHMARK: 5,000 Subs, 10,000 Invoices, 20,000 Payments");
        log.info("================================================================================");

        try {
            // 1. Seed controlled high-volume test dataset
            long seedStart = System.currentTimeMillis();
            seedTestDataset();
            long seedDuration = System.currentTimeMillis() - seedStart;
            log.info("Test dataset generated and committed in {} ms", seedDuration);

            // 2. Measure Dashboard Endpoint (without date filter)
            List<Long> dashTimings = new ArrayList<>();
            for (int i = 0; i < 15; i++) {
                long t0 = System.currentTimeMillis();
                MvcResult r = mockMvc.perform(get("/api/v1/reports/dashboard"))
                        .andExpect(status().isOk())
                        .andReturn();
                dashTimings.add(System.currentTimeMillis() - t0);
            }

            // 3. Measure Dashboard Endpoint with Date Range Filter
            List<Long> dateFilterTimings = new ArrayList<>();
            for (int i = 0; i < 15; i++) {
                long t0 = System.currentTimeMillis();
                MvcResult r = mockMvc.perform(get("/api/v1/reports/dashboard")
                                .param("startDate", "2026-01-01")
                                .param("endDate", "2026-12-31"))
                        .andExpect(status().isOk())
                        .andReturn();
                dateFilterTimings.add(System.currentTimeMillis() - t0);
            }

            // 4. Measure Paginated Payment Ledger (GET /api/v1/reports/payments?page=0&size=20)
            List<Long> paymentLedgerTimings = new ArrayList<>();
            for (int i = 0; i < 15; i++) {
                long t0 = System.currentTimeMillis();
                MvcResult r = mockMvc.perform(get("/api/v1/reports/payments")
                                .param("page", "0")
                                .param("size", "20"))
                        .andExpect(status().isOk())
                        .andReturn();
                paymentLedgerTimings.add(System.currentTimeMillis() - t0);
            }

            // 5. Measure Invoice Listing (GET /api/v1/invoices?page=0&size=20)
            List<Long> invoiceListTimings = new ArrayList<>();
            for (int i = 0; i < 15; i++) {
                long t0 = System.currentTimeMillis();
                MvcResult r = mockMvc.perform(get("/api/v1/invoices")
                                .param("page", "0")
                                .param("size", "20"))
                        .andExpect(status().isOk())
                        .andReturn();
                invoiceListTimings.add(System.currentTimeMillis() - t0);
            }

            // 6. Print Benchmark Results
            printMetrics("1. Financial Dashboard (/reports/dashboard)", dashTimings);
            printMetrics("2. Date-Filtered Dashboard (/reports/dashboard?dateRange)", dateFilterTimings);
            printMetrics("3. Paginated Payment Ledger (/reports/payments)", paymentLedgerTimings);
            printMetrics("4. Invoice Listing (/invoices)", invoiceListTimings);

            double dashP95 = calculateP95(dashTimings);
            log.info("Overall Dashboard P95 Response Time: {} ms (Target: < 2000 ms)", dashP95);
            assertTrue(dashP95 < 2000.0, "Dashboard P95 must be strictly below 2,000 ms (2.0s)");

        } finally {
            // Clean up seeded test data safely
            cleanupTestDataset();
        }
    }

    private void seedTestDataset() {
        log.info("Seeding {} subscriptions...", SUB_COUNT);
        String subSql = "INSERT INTO subscriptions (subscription_number, customer_id, customer_name, customer_email, " +
                "status, billing_interval, interval_count, start_date, current_period_start, current_period_end, " +
                "next_billing_date, subtotal, tax_amount, total_amount, currency, auto_renew, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        LocalDate now = LocalDate.now();
        Timestamp nowTs = Timestamp.valueOf(LocalDateTime.now());

        List<Object[]> subArgs = new ArrayList<>(SUB_COUNT);
        for (int i = 1; i <= SUB_COUNT; i++) {
            long custId = BENCH_CUSTOMER_START + (i % 500);
            String status = (i % 10 == 0) ? "CLOSED" : "ACTIVE";
            LocalDate start = now.minusMonths(i % 12);
            LocalDate periodEnd = start.plusMonths(1);
            LocalDate nextBill = periodEnd;

            subArgs.add(new Object[]{
                    "BENCH-SUB-" + String.format("%06d", i),
                    custId,
                    "Bench Corp " + custId,
                    "billing@bench" + custId + ".com",
                    status,
                    "MONTHLY",
                    1,
                    start,
                    start,
                    periodEnd,
                    nextBill,
                    5000.00,
                    900.00,
                    5900.00,
                    "INR",
                    1,
                    nowTs,
                    nowTs
            });

            if (subArgs.size() == 1000) {
                jdbcTemplate.batchUpdate(subSql, subArgs);
                subArgs.clear();
            }
        }
        if (!subArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(subSql, subArgs);
            subArgs.clear();
        }

        log.info("Seeding {} invoices...", INV_COUNT);
        String invSql = "INSERT INTO invoices (invoice_number, customer_id, customer_name, customer_email, " +
                "billing_period_start, billing_period_end, status, issue_date, due_date, subtotal, tax_amount, " +
                "total_amount, amount_paid, amount_due, currency, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> invArgs = new ArrayList<>(INV_COUNT);
        for (int i = 1; i <= INV_COUNT; i++) {
            long custId = BENCH_CUSTOMER_START + (i % 500);
            String status;
            double paid;
            double due;
            if (i % 3 == 0) {
                status = "PAID";
                paid = 5900.00;
                due = 0.00;
            } else if (i % 3 == 1) {
                status = "PARTIALLY_PAID";
                paid = 2000.00;
                due = 3900.00;
            } else {
                status = "UNPAID";
                paid = 0.00;
                due = 5900.00;
            }

            LocalDate issue = now.minusDays(i % 180);
            LocalDate dueDt = issue.plusDays(15);
            LocalDate pStart = issue.minusDays(30);

            invArgs.add(new Object[]{
                    "BENCH-INV-" + String.format("%06d", i),
                    custId,
                    "Bench Corp " + custId,
                    "billing@bench" + custId + ".com",
                    pStart,
                    issue,
                    status,
                    issue,
                    dueDt,
                    5000.00,
                    900.00,
                    5900.00,
                    paid,
                    due,
                    "INR",
                    nowTs,
                    nowTs
            });

            if (invArgs.size() == 1000) {
                jdbcTemplate.batchUpdate(invSql, invArgs);
                invArgs.clear();
            }
        }
        if (!invArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(invSql, invArgs);
            invArgs.clear();
        }

        log.info("Seeding {} payments...", PMT_COUNT);
        Long minInvId = jdbcTemplate.queryForObject("SELECT COALESCE(MIN(id), 1) FROM invoices WHERE invoice_number LIKE 'BENCH-INV-%'", Long.class);
        if (minInvId == null) minInvId = 1L;

        String pmtSql = "INSERT INTO payments (payment_number, invoice_id, customer_id, amount, " +
                "payment_date, transaction_reference, status, currency, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        List<Object[]> pmtArgs = new ArrayList<>(PMT_COUNT);
        for (int i = 1; i <= PMT_COUNT; i++) {
            long custId = BENCH_CUSTOMER_START + (i % 500);
            long targetInvoiceId = minInvId + (i % INV_COUNT);
            Timestamp pmtDt = Timestamp.valueOf(LocalDateTime.now().minusHours(i % 720));

            pmtArgs.add(new Object[]{
                    "BENCH-PAY-" + String.format("%06d", i),
                    targetInvoiceId,
                    custId,
                    2000.00,
                    pmtDt,
                    "BENCH-TXN-" + String.format("%06d", i),
                    "SUCCESS",
                    "INR",
                    nowTs,
                    nowTs
            });

            if (pmtArgs.size() == 2000) {
                jdbcTemplate.batchUpdate(pmtSql, pmtArgs);
                pmtArgs.clear();
            }
        }
        if (!pmtArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(pmtSql, pmtArgs);
            pmtArgs.clear();
        }

        log.info("Database seeding complete: 5,000 subscriptions, 10,000 invoices, 20,000 payments ready.");
    }

    private void cleanupTestDataset() {
        log.info("Cleaning up benchmark dataset (customer_id >= {})...", BENCH_CUSTOMER_START);
        jdbcTemplate.update("DELETE FROM payments WHERE transaction_reference LIKE 'BENCH-TXN-%'");
        jdbcTemplate.update("DELETE FROM invoices WHERE invoice_number LIKE 'BENCH-INV-%'");
        jdbcTemplate.update("DELETE FROM subscriptions WHERE subscription_number LIKE 'BENCH-SUB-%'");
        log.info("Cleanup completed successfully.");
    }

    private void printMetrics(String name, List<Long> timings) {
        Collections.sort(timings);
        double avg = timings.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long median = timings.get(timings.size() / 2);
        long p95 = timings.get((int) Math.ceil(timings.size() * 0.95) - 1);
        long max = timings.get(timings.size() - 1);

        log.info("--------------------------------------------------------------------------------");
        log.info("BENCHMARK METRICS for: {}", name);
        log.info("  Iterations : {}", timings.size());
        log.info("  Average    : {} ms", String.format("%.2f", avg));
        log.info("  Median     : {} ms", median);
        log.info("  p95        : {} ms", p95);
        log.info("  Max        : {} ms", max);
        log.info("--------------------------------------------------------------------------------");
    }

    private double calculateP95(List<Long> timings) {
        Collections.sort(timings);
        return timings.get((int) Math.ceil(timings.size() * 0.95) - 1);
    }
}
