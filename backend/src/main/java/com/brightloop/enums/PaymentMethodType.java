package com.brightloop.enums;

import lombok.Getter;

/**
 * Payment method types supported across the BrightLoop system:
 * CASH, CARD, BANK_TRANSFER, UPI
 */
@Getter
public enum PaymentMethodType {
    CASH("Cash"),
    CARD("Card (Credit/Debit)"),
    CREDIT_CARD("Credit Card"),
    BANK_TRANSFER("Bank Transfer"),
    UPI("UPI"),
    OTHER("Other");

    private final String displayName;

    PaymentMethodType(String displayName) {
        this.displayName = displayName;
    }
}
