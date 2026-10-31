package com.brightloop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request payload for renewing an active or expiring subscription contract.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRenewalRequest {
    private Integer duration;
    private String durationUnit;
    private LocalDate newEndDate;
    private Boolean autoRenew;
    private String notes;
}
