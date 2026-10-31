package com.brightloop.controller;

import com.brightloop.dto.DiscountRequest;
import com.brightloop.dto.DiscountResponse;
import com.brightloop.service.DiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/discounts")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DiscountController {

    private final DiscountService discountService;

    @PostMapping
    public ResponseEntity<DiscountResponse> createDiscount(@Valid @RequestBody DiscountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(discountService.createDiscount(request));
    }

    @GetMapping
    public ResponseEntity<Page<DiscountResponse>> getDiscounts(
            @RequestParam(required = false, defaultValue = "true") Boolean activeOnly,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(discountService.getDiscounts(activeOnly, pageable));
    }

    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> applyDiscount(
            @RequestParam String code,
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") Integer quantity,
            @RequestParam BigDecimal purchaseAmount,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long subscriptionId,
            @RequestParam(defaultValue = "false") boolean recordUsage) {
        return ResponseEntity.ok(discountService.applyDiscount(
                code, productId, quantity, purchaseAmount, customerId, subscriptionId, recordUsage));
    }
}