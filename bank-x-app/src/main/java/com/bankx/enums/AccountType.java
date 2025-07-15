package com.bankx.enums;

public enum AccountType {
    CURRENT("Current Account"),
    SAVINGS("Savings Account");

    private final String displayName;

    AccountType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}