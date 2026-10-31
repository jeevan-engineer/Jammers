package com.brightloop.repository;

import com.brightloop.entity.Invoice;
import com.brightloop.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    boolean existsByInvoiceNumber(String invoiceNumber);

    boolean existsBySubscriptionIdAndBillingPeriodStartAndBillingPeriodEnd(
            Long subscriptionId,
            LocalDate billingPeriodStart,
            LocalDate billingPeriodEnd
    );

    Page<Invoice> findByCustomerId(Long customerId, Pageable pageable);

    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);

    Page<Invoice> findByStatusAndCustomerId(InvoiceStatus status, Long customerId, Pageable pageable);

    List<Invoice> findBySubscriptionId(Long subscriptionId);

    long countByStatus(InvoiceStatus status);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.dueDate < :today AND i.status IN :unsettledStatuses")
    long countOverdue(
            @Param("today") LocalDate today,
            @Param("unsettledStatuses") Collection<InvoiceStatus> unsettledStatuses
    );

    @Query("SELECT i FROM Invoice i WHERE i.dueDate < :today AND i.status IN :unsettledStatuses")
    List<Invoice> findOverdue(
            @Param("today") LocalDate today,
            @Param("unsettledStatuses") Collection<InvoiceStatus> unsettledStatuses
    );

    @Query("SELECT COALESCE(SUM(i.amountPaid), 0) FROM Invoice i")
    BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(i.amountDue), 0) FROM Invoice i WHERE i.status IN :unsettledStatuses")
    BigDecimal sumAmountOwed(@Param("unsettledStatuses") Collection<InvoiceStatus> unsettledStatuses);
}
