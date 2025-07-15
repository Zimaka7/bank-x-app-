package com.bankx.enums;

public enum NotificationStatus {
    SENT("Sent"),
    DELIVERED("Delivered"),
    FAILED("Failed");

    private final String displayName;

    NotificationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}