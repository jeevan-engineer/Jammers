package com.brightloop.service;

import com.brightloop.dto.DiscountRequest;
import com.brightloop.dto.DiscountResponse;
import com.brightloop.entity.Discount;
import com.brightloop.enums.DiscountType;
import com.brightloop.entity.DiscountUsage;
import com.brightloop.entity.Product;
import com.brightloop.repository.DiscountRepo;
import com.brightloop.repository.DiscountUsageRepo;
import com.brightloop.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscountService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final DiscountRepo discountRepository;
    private final DiscountUsageRepo discountUsageRepository;
    private final ProductRepository productRepository;

    @Transactional
    public DiscountResponse createDiscount(DiscountRequest request) {
        if (discountRepository.existsByCodeIgnoreCase(request.getCode().trim())) {
            throw new IllegalArgumentException("Discount code already exists: " + request.getCode());
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("endDate cannot be earlier than startDate");
        }
        if (request.getDiscountType() == DiscountType.PERCENTAGE
                && request.getDiscountValue().compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalArgumentException("Percentage discount cannot exceed 100%");
        }

        Set<Product> products = new HashSet<>();
        if (request.getApplicableProductIds() != null && !request.getApplicableProductIds().isEmpty()) {
            products.addAll(productRepository.findAllById(request.getApplicableProductIds()));
        }

        Discount discount = Discount.builder()
                .code(request.getCode().trim().toUpperCase())
                .name(request.getName().trim())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minPurchaseAmount(request.getMinPurchaseAmount() != null ? request.getMinPurchaseAmount() : BigDecimal.ZERO)
                .minQuantity(request.getMinQuantity() != null ? request.getMinQuantity() : 1)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .usageLimit(request.getUsageLimit())
                .timesUsed(0)
                .applicableProducts(products)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        return mapToResponse(discountRepository.save(discount));
    }

    @Transactional(readOnly = true)
    public Page<DiscountResponse> getDiscounts(Boolean activeOnly, Pageable pageable) {
        Page<Discount> discounts = Boolean.TRUE.equals(activeOnly)
                ? discountRepository.findByActiveTrue(pageable)
                : discountRepository.findAll(pageable);

        return discounts.map(this::mapToResponse);
    }

    @Transactional
    public Map<String, Object> applyDiscount(
            String code, Long productId, Integer quantity, BigDecimal purchaseAmount,
            Long customerId, Long subscriptionId, boolean recordUsage) {

        Discount discount = discountRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new EntityNotFoundException("Invalid discount code: " + code));

        LocalDate today = LocalDate.now();
        if (!Boolean.TRUE.equals(discount.getActive())) {
            throw new IllegalArgumentException("Discount code is inactive");
        }
        if (today.isBefore(discount.getStartDate()) || today.isAfter(discount.getEndDate())) {
            throw new IllegalArgumentException("Discount code is expired or not yet active");
        }
        if (discount.getUsageLimit() != null && discount.getTimesUsed() >= discount.getUsageLimit()) {
            throw new IllegalArgumentException("Discount usage limit reached");
        }
        if (purchaseAmount.compareTo(discount.getMinPurchaseAmount()) < 0) {
            throw new IllegalArgumentException("Minimum purchase amount of " + discount.getMinPurchaseAmount() + " not met");
        }
        if (quantity < discount.getMinQuantity()) {
            throw new IllegalArgumentException("Minimum quantity of " + discount.getMinQuantity() + " not met");
        }
        if (!discount.getApplicableProducts().isEmpty()) {
            boolean matchesProduct = discount.getApplicableProducts().stream()
                    .anyMatch(p -> p.getId().equals(productId));
            if (!matchesProduct) {
                throw new IllegalArgumentException("Discount does not apply to this product");
            }
        }

        BigDecimal discountAmount;
        if (discount.getDiscountType() == DiscountType.PERCENTAGE) {
            discountAmount = purchaseAmount.multiply(discount.getDiscountValue())
                    .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        } else {
            discountAmount = discount.getDiscountValue().min(purchaseAmount).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal finalAmount = purchaseAmount.subtract(discountAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        if (recordUsage) {
            discount.setTimesUsed(discount.getTimesUsed() + 1);
            discountRepository.save(discount);

            discountUsageRepository.save(DiscountUsage.builder()
                    .discount(discount)
                    .customerId(customerId)
                    .subscriptionId(subscriptionId)
                    .build());
        }

        return Map.of(
                "discountId", discount.getId(),
                "code", discount.getCode(),
                "discountType", discount.getDiscountType(),
                "originalAmount", purchaseAmount.setScale(2, RoundingMode.HALF_UP),
                "discountAmount", discountAmount,
                "finalAmount", finalAmount
        );
    }

    private DiscountResponse mapToResponse(Discount discount) {
        Set<Long> productIds = discount.getApplicableProducts().stream()
                .map(Product::getId)
                .collect(Collectors.toSet());

        return DiscountResponse.builder()
                .id(discount.getId())
                .code(discount.getCode())
                .name(discount.getName())
                .discountType(discount.getDiscountType())
                .discountValue(discount.getDiscountValue())
                .minPurchaseAmount(discount.getMinPurchaseAmount())
                .minQuantity(discount.getMinQuantity())
                .startDate(discount.getStartDate())
                .endDate(discount.getEndDate())
                .usageLimit(discount.getUsageLimit())
                .timesUsed(discount.getTimesUsed())
                .applicableProductIds(productIds)
                .active(discount.getActive())
                .createdAt(discount.getCreatedAt())
                .build();
    }
}