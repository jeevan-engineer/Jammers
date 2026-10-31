package com.brightloop.repository;

import com.brightloop.entity.DiscountUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscountUsageRepo extends JpaRepository<DiscountUsage, Long> {
}