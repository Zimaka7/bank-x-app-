package com.bankx.enums;

public enum TransactionType {
    CREDIT("Credit"),
    DEBIT("Debit"),
    TRANSFER("Transfer");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}