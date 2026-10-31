package com.brightloop.repository;

import com.brightloop.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentNumber(String paymentNumber);

    boolean existsByPaymentNumber(String paymentNumber);

    boolean existsByTransactionReference(String transactionReference);

    List<Payment> findByInvoiceId(Long invoiceId);

    Page<Payment> findByCustomerId(Long customerId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS'")
    BigDecimal sumTotalCollected();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS' AND p.paymentDate >= :start AND p.paymentDate <= :end")
    BigDecimal sumTotalCollectedBetween(@org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start, @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

    List<Payment> findTop10ByOrderByPaymentDateDesc();

    Page<Payment> findAllByOrderByPaymentDateDesc(Pageable pageable);
}
