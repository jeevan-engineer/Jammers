package com.brightloop.controller;

import com.brightloop.dto.ProductVariantRequest;
import com.brightloop.dto.ProductVariantResponse;
import com.brightloop.service.ProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductVariantController {

    private final ProductVariantService variantService;

    @PostMapping("/api/products/{productId}/variants")
    public ResponseEntity<ProductVariantResponse> createVariant(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(variantService.createVariant(productId, request));
    }

    @GetMapping("/api/products/{productId}/variants")
    public ResponseEntity<List<ProductVariantResponse>> getVariants(
            @PathVariable Long productId) {

        return ResponseEntity.ok(
                variantService.getVariants(productId));
    }

    @GetMapping("/api/variants/{variantId}")
    public ResponseEntity<ProductVariantResponse> getVariant(
            @PathVariable Long variantId) {

        return ResponseEntity.ok(
                variantService.getVariantById(variantId));
    }

    @PutMapping("/api/variants/{variantId}")
    public ResponseEntity<ProductVariantResponse> updateVariant(
            @PathVariable Long variantId,
            @Valid @RequestBody ProductVariantRequest request) {

        return ResponseEntity.ok(
                variantService.updateVariant(variantId, request));
    }

    @DeleteMapping("/api/variants/{variantId}")
    public ResponseEntity<Void> deactivateVariant(
            @PathVariable Long variantId) {

        variantService.deactivateVariant(variantId);

        return ResponseEntity.noContent().build();
    }
}