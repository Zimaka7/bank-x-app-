package com.bankx.enums;

public enum NotificationType {
    WELCOME("Welcome"),
    TRANSACTION("Transaction"),
    ACCOUNT_CREDIT("Account Credit"),
    ACCOUNT_DEBIT("Account Debit"),
    TRANSFER("Transfer"),
    PAYMENT("Payment"),
    INTEREST("Interest"),
    FEE("Fee"),
    OTP("OTP");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}