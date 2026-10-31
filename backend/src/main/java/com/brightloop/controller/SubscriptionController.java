package com.brightloop.controller;

import com.brightloop.dto.SubscriptionCreateRequest;
import com.brightloop.dto.SubscriptionResponse;
import com.brightloop.dto.SubscriptionStatusUpdateRequest;
import com.brightloop.service.SubscriptionService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/subscriptions")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @Valid @RequestBody SubscriptionCreateRequest request
    ) {
        SubscriptionResponse response = subscriptionService.createSubscription(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<SubscriptionResponse>> getAllSubscriptions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long customerId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<SubscriptionResponse> subscriptions = subscriptionService.getAllSubscriptions(status, customerId, pageable);
        return ResponseEntity.ok(subscriptions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> getSubscriptionById(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionById(id));
    }

    @GetMapping("/number/{subscriptionNumber}")
    public ResponseEntity<SubscriptionResponse> getSubscriptionByNumber(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionByNumber(subscriptionNumber));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SubscriptionResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(subscriptionService.updateStatus(id, request));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<SubscriptionResponse> closeSubscription(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Closed via API request") String reason
    ) {
        return ResponseEntity.ok(subscriptionService.closeSubscription(id, reason));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<SubscriptionResponse> pauseSubscription(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Paused via API request") String reason
    ) {
        return ResponseEntity.ok(subscriptionService.pauseSubscription(id, reason));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<SubscriptionResponse> resumeSubscription(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.resumeSubscription(id));
    }

    @PostMapping("/{id}/renew")
    public ResponseEntity<SubscriptionResponse> renewSubscription(
            @PathVariable Long id,
            @RequestBody(required = false) com.brightloop.dto.SubscriptionRenewalRequest request
    ) {
        return ResponseEntity.ok(subscriptionService.renewSubscription(id, request));
    }

    @PostMapping("/{id}/generate-invoice")
    public ResponseEntity<Map<String, Object>> generateSingleInvoice(@PathVariable Long id) {
        Long invoiceId = subscriptionService.generateSingleInvoice(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Invoice generated successfully for subscription",
                "invoiceId", invoiceId
        ));
    }

    @PostMapping("/generate-billing")
    public ResponseEntity<Map<String, Object>> generateBillingInvoices() {
        List<Long> generatedIds = subscriptionService.generateBillingInvoices();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Generated invoices for due subscriptions",
                "invoicesGeneratedCount", generatedIds.size(),
                "invoiceIds", generatedIds
        ));
    }
}
