package com.brightloop.controller;

import com.brightloop.dto.InvoiceCreateRequest;
import com.brightloop.dto.InvoiceResponse;
import com.brightloop.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invoices")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody InvoiceCreateRequest request) {
        InvoiceResponse response = invoiceService.createInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<InvoiceResponse>> getAllInvoices(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long customerId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<InvoiceResponse> invoices = invoiceService.getAllInvoices(status, customerId, pageable);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @GetMapping("/number/{invoiceNumber}")
    public ResponseEntity<InvoiceResponse> getInvoiceByNumber(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(invoiceService.getInvoiceByNumber(invoiceNumber));
    }

    @GetMapping("/subscription/{subscriptionId}")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesBySubscriptionId(@PathVariable Long subscriptionId) {
        return ResponseEntity.ok(invoiceService.getInvoicesBySubscriptionId(subscriptionId));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<InvoiceResponse>> getOverdueInvoices() {
        return ResponseEntity.ok(invoiceService.getOverdueInvoices());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<InvoiceResponse> updateInvoiceStatus(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body
    ) {
        String statusStr = body != null ? body.get("status") : null;
        if ("CANCELLED".equalsIgnoreCase(statusStr)) {
            String reason = body.getOrDefault("reason", "Cancelled via API");
            return ResponseEntity.ok(invoiceService.cancelInvoice(id, reason));
        } else if ("CONFIRMED".equalsIgnoreCase(statusStr)) {
            return ResponseEntity.ok(invoiceService.confirmInvoice(id));
        }
        throw new com.brightloop.exception.BusinessValidationException("Unsupported invoice status transition: " + statusStr);
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<InvoiceResponse> confirmInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.confirmInvoice(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<InvoiceResponse> cancelInvoice(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Cancelled via API") String reason
    ) {
        return ResponseEntity.ok(invoiceService.cancelInvoice(id, reason));
    }

    @GetMapping(value = "/{id}/print", produces = "text/plain")
    public ResponseEntity<String> printInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.generatePrintableInvoice(id));
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<java.util.Map<String, Object>> sendInvoice(@PathVariable Long id) {
        invoiceService.sendInvoice(id);
        return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Invoice notification dispatched successfully"
        ));
    }
}
