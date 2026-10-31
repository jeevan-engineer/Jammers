package com.brightloop.service;

import com.brightloop.dto.ProductVariantRequest;
import com.brightloop.dto.ProductVariantResponse;
import com.brightloop.entity.Product;
import com.brightloop.entity.ProductVariant;
import com.brightloop.repository.ProductVariantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final ProductService productService;

    @Transactional
    public ProductVariantResponse createVariant(
            Long productId,
            ProductVariantRequest request) {

        Product product = productService.findProductOrThrow(productId);

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .attribute(request.getAttribute().trim())
                .value(request.getValue().trim())
                .extraPrice(request.getExtraPrice())
                .active(request.getActive() != null
                        ? request.getActive() : true)
                .build();

        return mapToResponse(variantRepository.save(variant));
    }

    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getVariants(Long productId) {

        productService.findProductOrThrow(productId);

        return variantRepository.findByProductIdAndActiveTrue(productId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductVariantResponse getVariantById(Long variantId) {

        return mapToResponse(findVariantOrThrow(variantId));
    }

    @Transactional
    public ProductVariantResponse updateVariant(
            Long variantId,
            ProductVariantRequest request) {

        ProductVariant variant = findVariantOrThrow(variantId);

        variant.setAttribute(request.getAttribute().trim());
        variant.setValue(request.getValue().trim());
        variant.setExtraPrice(request.getExtraPrice());

        if (request.getActive() != null) {
            variant.setActive(request.getActive());
        }

        return mapToResponse(variantRepository.save(variant));
    }

    @Transactional
    public void deactivateVariant(Long variantId) {

        ProductVariant variant = findVariantOrThrow(variantId);

        variant.setActive(false);

        variantRepository.save(variant);
    }

    private ProductVariant findVariantOrThrow(Long id) {

        return variantRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Product variant not found with id: " + id));
    }

    private ProductVariantResponse mapToResponse(ProductVariant variant) {

        Product product = variant.getProduct();

        BigDecimal effectivePrice =
                product.getSalesPrice().add(variant.getExtraPrice());

        return ProductVariantResponse.builder()
                .id(variant.getId())
                .productId(product.getId())
                .productName(product.getName())
                .attribute(variant.getAttribute())
                .value(variant.getValue())
                .extraPrice(variant.getExtraPrice())
                .effectivePrice(effectivePrice)
                .active(variant.getActive())
                .createdAt(variant.getCreatedAt())
                .updatedAt(variant.getUpdatedAt())
                .build();
    }
}