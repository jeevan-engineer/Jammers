package com.brightloop.repository;

import com.brightloop.entity.Subscription;
import com.brightloop.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findBySubscriptionNumber(String subscriptionNumber);

    boolean existsBySubscriptionNumber(String subscriptionNumber);

    boolean existsByQuotationTemplateId(Long quotationTemplateId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subscription s WHERE s.id = :id")
    Optional<Subscription> findByIdForUpdate(@Param("id") Long id);

    Page<Subscription> findByCustomerId(Long customerId, Pageable pageable);

    Page<Subscription> findByStatus(SubscriptionStatus status, Pageable pageable);

    Page<Subscription> findByStatusAndCustomerId(SubscriptionStatus status, Long customerId, Pageable pageable);

    long countByStatus(SubscriptionStatus status);

    @Query("SELECT s FROM Subscription s WHERE s.status = :status AND s.pausedAt IS NULL AND s.nextBillingDate <= :targetDate")
    List<Subscription> findDueForBilling(
            @Param("status") SubscriptionStatus status,
            @Param("targetDate") LocalDate targetDate
    );

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Subscription s WHERE s.status = :status")
    BigDecimal sumTotalAmountByStatus(@Param("status") SubscriptionStatus status);
}
