package com.brightloop.enums;

import lombok.Getter;

/**
 * Lifecycle status of a subscription in BrightLoop aligned with hackathon specifications:
 * Draft -> Quotation -> Confirmed -> Active -> Closed
 */
@Getter
public enum SubscriptionStatus {
    DRAFT("Draft", "Initial draft of the subscription contract"),
    QUOTATION("Quotation", "Proposed quote sent to customer for review and approval"),
    CONFIRMED("Confirmed", "Customer accepted quote; awaiting activation or first cycle"),
    ACTIVE("Active", "Subscription is currently active and in billing lifecycle"),
    CLOSED("Closed", "Subscription has ended, expired, or been terminated");

    private final String displayName;
    private final String description;

    SubscriptionStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean canTransitionTo(SubscriptionStatus nextStatus) {
        if (this == nextStatus) {
            return false;
        }

        return switch (this) {
            case DRAFT -> nextStatus == QUOTATION || nextStatus == CLOSED;
            case QUOTATION -> nextStatus == CONFIRMED || nextStatus == CLOSED;
            case CONFIRMED -> nextStatus == ACTIVE || nextStatus == CLOSED;
            case ACTIVE -> nextStatus == CLOSED;
            case CLOSED -> false;
        };
    }
}
