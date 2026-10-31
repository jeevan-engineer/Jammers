package com.brightloop.service;

import com.brightloop.dto.*;
import com.brightloop.entity.Tax;
import com.brightloop.enums.ProductType;
import com.brightloop.repository.TaxRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxService {

    private final TaxRepository taxRepository;

    @Transactional
    public TaxResponse createTax(TaxRequest request) {

        Tax tax = Tax.builder()
                .name(request.getName().trim())
                .percentage(request.getPercentage())
                .applicability(request.getApplicability())
                .active(request.getActive() != null
                        ? request.getActive() : true)
                .build();

        return mapToResponse(taxRepository.save(tax));
    }

    @Transactional(readOnly = true)
    public List<TaxResponse> getAllTaxes(Boolean activeOnly) {

        List<Tax> taxes = Boolean.TRUE.equals(activeOnly)
                ? taxRepository.findByActiveTrue()
                : taxRepository.findAll();

        return taxes.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaxResponse getTaxById(Long id) {

        return mapToResponse(findTaxOrThrow(id));
    }

    @Transactional
    public TaxResponse updateTax(Long id, TaxRequest request) {

        Tax tax = findTaxOrThrow(id);

        tax.setName(request.getName().trim());
        tax.setPercentage(request.getPercentage());
        tax.setApplicability(request.getApplicability());

        if (request.getActive() != null) {
            tax.setActive(request.getActive());
        }

        return mapToResponse(taxRepository.save(tax));
    }

    @Transactional
    public void deactivateTax(Long id) {

        Tax tax = findTaxOrThrow(id);

        tax.setActive(false);

        taxRepository.save(tax);
    }

    @Transactional(readOnly = true)
    public TaxCalculationResponse calculateTax(
            Long id,
            TaxCalculationRequest request) {

        Tax tax = findTaxOrThrow(id);

        if (!tax.getActive()) {
            throw new IllegalArgumentException("Tax is inactive");
        }

        if (tax.getApplicability() != request.getProductType()) {
            throw new IllegalArgumentException(
                    "Tax is not applicable to this product type");
        }

        BigDecimal taxAmount = request.getBaseAmount()
                .multiply(tax.getPercentage())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal totalAmount = request.getBaseAmount()
                .add(taxAmount);

        return TaxCalculationResponse.builder()
                .taxId(tax.getId())
                .taxName(tax.getName())
                .baseAmount(request.getBaseAmount())
                .percentage(tax.getPercentage())
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .applicability(tax.getApplicability())
                .build();
    }

    private Tax findTaxOrThrow(Long id) {

        return taxRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Tax not found with id: " + id));
    }

    private TaxResponse mapToResponse(Tax tax) {

        return TaxResponse.builder()
                .id(tax.getId())
                .name(tax.getName())
                .percentage(tax.getPercentage())
                .applicability(tax.getApplicability())
                .active(tax.getActive())
                .createdAt(tax.getCreatedAt())
                .updatedAt(tax.getUpdatedAt())
                .build();
    }
}