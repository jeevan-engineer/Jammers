package com.brightloop.repository;

import com.brightloop.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByCustomerId(Long customerId);

    List<PaymentMethod> findByCustomerIdAndIsActiveTrue(Long customerId);

    Optional<PaymentMethod> findByCustomerIdAndIsDefaultTrue(Long customerId);
}
