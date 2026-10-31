package com.brightloop.service;

import com.brightloop.dto.ProductRequest;
import com.brightloop.dto.ProductResponse;
import com.brightloop.entity.Product;
import com.brightloop.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName().trim())
                .type(request.getType())
                .salesPrice(request.getSalesPrice())
                .costPrice(request.getCostPrice())
                .notes(request.getNotes())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        return mapToResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(Boolean activeOnly, Pageable pageable) {
        Page<Product> products = Boolean.TRUE.equals(activeOnly)
                ? productRepository.findByActiveTrue(pageable)
                : productRepository.findAll(pageable);

        return products.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return mapToResponse(findProductOrThrow(id));
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findProductOrThrow(id);
        product.setName(request.getName().trim());
        product.setType(request.getType());
        product.setSalesPrice(request.getSalesPrice());
        product.setCostPrice(request.getCostPrice());
        product.setNotes(request.getNotes());
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        return mapToResponse(productRepository.save(product));
    }

    @Transactional
    public void deactivateProduct(Long id) {
        Product product = findProductOrThrow(id);
        product.setActive(false);
        productRepository.save(product);
    }

    public Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .type(product.getType())
                .salesPrice(product.getSalesPrice())
                .costPrice(product.getCostPrice())
                .notes(product.getNotes())
                .active(product.getActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}