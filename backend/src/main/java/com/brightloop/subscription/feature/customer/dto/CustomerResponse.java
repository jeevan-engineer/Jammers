package com.brightloop.subscription.feature.customer.dto;

import java.time.LocalDateTime;

/**
 * Customer response DTO — returned by all customer endpoints.
 * linkedUserId is nullable (null when customer has no user account).
 * This field is used by Member 4 to link invoices to the user's portal session.
 */
public record CustomerResponse(
        Long id,
        Long linkedUserId,
        String name,
        String email,
        String phone,
        String companyName,
        String address,
        String taxId,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
