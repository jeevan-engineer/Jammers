package com.brightloop.repository;

import com.brightloop.entity.Plan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    Page<Plan> findByActiveTrue(Pageable pageable);

    Page<Plan> findByProductIdAndActiveTrue(Long productId, Pageable pageable);
}