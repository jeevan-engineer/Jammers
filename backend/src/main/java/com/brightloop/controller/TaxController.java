package com.brightloop.controller;

import com.brightloop.dto.*;
import com.brightloop.service.TaxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/taxes")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TaxController {

    private final TaxService taxService;

    @PostMapping
    public ResponseEntity<TaxResponse> createTax(
            @Valid @RequestBody TaxRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taxService.createTax(request));
    }

    @GetMapping
    public ResponseEntity<List<TaxResponse>> getAllTaxes(
            @RequestParam(defaultValue = "true") Boolean activeOnly) {

        return ResponseEntity.ok(
                taxService.getAllTaxes(activeOnly));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaxResponse> getTaxById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                taxService.getTaxById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaxResponse> updateTax(
            @PathVariable Long id,
            @Valid @RequestBody TaxRequest request) {

        return ResponseEntity.ok(
                taxService.updateTax(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateTax(
            @PathVariable Long id) {

        taxService.deactivateTax(id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/calculate")
    public ResponseEntity<TaxCalculationResponse> calculateTax(
            @PathVariable Long id,
            @Valid @RequestBody TaxCalculationRequest request) {

        return ResponseEntity.ok(
                taxService.calculateTax(id, request));
    }
}