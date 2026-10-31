package com.brightloop.service;

import com.brightloop.dto.SubscriptionCreateRequest;
import com.brightloop.dto.SubscriptionResponse;
import com.brightloop.dto.SubscriptionStatusUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SubscriptionService {

    SubscriptionResponse createSubscription(SubscriptionCreateRequest request);

    SubscriptionResponse getSubscriptionById(Long id);

    SubscriptionResponse getSubscriptionByNumber(String subscriptionNumber);

    Page<SubscriptionResponse> getAllSubscriptions(String status, Long customerId, Pageable pageable);

    SubscriptionResponse updateStatus(Long id, SubscriptionStatusUpdateRequest request);

    SubscriptionResponse closeSubscription(Long id, String reason);

    SubscriptionResponse generateQuotation(Long templateId, com.brightloop.dto.QuotationGenerateRequest request);

    SubscriptionResponse pauseSubscription(Long id, String reason);

    SubscriptionResponse resumeSubscription(Long id);

    SubscriptionResponse renewSubscription(Long id, com.brightloop.dto.SubscriptionRenewalRequest request);

    Long generateSingleInvoice(Long id);

    /**
     * Executes automated periodic billing cycle generation:
     * finds active subscriptions due for billing, validates anti-double-billing rules,
     * generates invoices, and advances the subscription next billing date.
     */
    List<Long> generateBillingInvoices();
}
