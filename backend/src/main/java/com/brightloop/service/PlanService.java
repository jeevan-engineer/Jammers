package com.brightloop.service;

import com.brightloop.dto.PlanRequest;
import com.brightloop.dto.PlanResponse;
import com.brightloop.entity.Plan;
import com.brightloop.entity.Product;
import com.brightloop.repository.PlanRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final ProductService productService;

    @Transactional
    public PlanResponse createPlan(PlanRequest request) {
        validateDates(request);
        Product product = productService.findProductOrThrow(request.getProductId());

        Plan plan = Plan.builder()
                .product(product)
                .name(request.getName().trim())
                .price(request.getPrice())
                .billingPeriod(request.getBillingPeriod())
                .minQuantity(request.getMinQuantity() != null ? request.getMinQuantity() : 1)
                .availableFrom(request.getAvailableFrom())
                .availableTo(request.getAvailableTo())
                .autoClose(Boolean.TRUE.equals(request.getAutoClose()))
                .earlyClose(request.getEarlyClose() != null ? request.getEarlyClose() : true)
                .pausable(request.getPausable() != null ? request.getPausable() : true)
                .renewable(request.getRenewable() != null ? request.getRenewable() : true)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        return mapToResponse(planRepository.save(plan));
    }

    @Transactional(readOnly = true)
    public Page<PlanResponse> getPlans(Long productId, Boolean activeOnly, Pageable pageable) {
        Page<Plan> plans;
        if (productId != null) {
            plans = planRepository.findByProductIdAndActiveTrue(productId, pageable);
        } else if (Boolean.TRUE.equals(activeOnly)) {
            plans = planRepository.findByActiveTrue(pageable);
        } else {
            plans = planRepository.findAll(pageable);
        }
        return plans.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public PlanResponse getPlanById(Long id) {
        return mapToResponse(findPlanOrThrow(id));
    }

    @Transactional
    public PlanResponse updatePlan(Long id, PlanRequest request) {
        validateDates(request);
        Plan plan = findPlanOrThrow(id);
        Product product = productService.findProductOrThrow(request.getProductId());

        plan.setProduct(product);
        plan.setName(request.getName().trim());
        plan.setPrice(request.getPrice());
        plan.setBillingPeriod(request.getBillingPeriod());
        plan.setMinQuantity(request.getMinQuantity());
        plan.setAvailableFrom(request.getAvailableFrom());
        plan.setAvailableTo(request.getAvailableTo());
        if (request.getAutoClose() != null) plan.setAutoClose(request.getAutoClose());
        if (request.getEarlyClose() != null) plan.setEarlyClose(request.getEarlyClose());
        if (request.getPausable() != null) plan.setPausable(request.getPausable());
        if (request.getRenewable() != null) plan.setRenewable(request.getRenewable());
        if (request.getActive() != null) plan.setActive(request.getActive());

        return mapToResponse(planRepository.save(plan));
    }

    public Plan findPlanOrThrow(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plan not found with id: " + id));
    }

    private void validateDates(PlanRequest request) {
        if (request.getAvailableFrom() != null && request.getAvailableTo() != null
                && request.getAvailableTo().isBefore(request.getAvailableFrom())) {
            throw new IllegalArgumentException("availableTo date cannot be earlier than availableFrom date");
        }
    }

    private PlanResponse mapToResponse(Plan plan) {
        return PlanResponse.builder()
                .id(plan.getId())
                .productId(plan.getProduct().getId())
                .productName(plan.getProduct().getName())
                .name(plan.getName())
                .price(plan.getPrice())
                .billingPeriod(plan.getBillingPeriod())
                .minQuantity(plan.getMinQuantity())
                .availableFrom(plan.getAvailableFrom())
                .availableTo(plan.getAvailableTo())
                .autoClose(plan.getAutoClose())
                .earlyClose(plan.getEarlyClose())
                .pausable(plan.getPausable())
                .renewable(plan.getRenewable())
                .active(plan.getActive())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }
}