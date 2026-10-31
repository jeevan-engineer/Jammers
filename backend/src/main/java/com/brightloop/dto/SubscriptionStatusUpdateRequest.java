package com.brightloop.dto;

import com.brightloop.enums.SubscriptionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for updating a subscription's status in its lifecycle.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionStatusUpdateRequest {

    @NotNull(message = "Target status is required")
    private SubscriptionStatus status;

    private String changedBy;
    private String reason;
    private String comment;
}
