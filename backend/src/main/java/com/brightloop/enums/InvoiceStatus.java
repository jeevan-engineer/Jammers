package com.brightloop.enums;

import lombok.Getter;

/**
 * Invoice lifecycle statuses aligned with hackathon specifications:
 * DRAFT, CONFIRMED, UNPAID, PARTIALLY_PAID, PAID, CANCELLED
 */
@Getter
public enum InvoiceStatus {
    DRAFT("Draft", "Invoice is being drafted and can be edited"),
    CONFIRMED("Confirmed", "Invoice is confirmed and finalized"),
    UNPAID("Unpaid", "Invoice is posted/issued and awaiting payment"),
    PARTIALLY_PAID("Partially Paid", "Invoice has received partial payment"),
    PAID("Paid", "Invoice has been paid in full"),
    CANCELLED("Cancelled", "Invoice has been cancelled/voided");

    private final String displayName;
    private final String description;

    InvoiceStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public boolean isPayable() {
        return this == CONFIRMED || this == UNPAID || this == PARTIALLY_PAID;
    }

    public boolean isSettled() {
        return this == PAID || this == CANCELLED;
    }
}
