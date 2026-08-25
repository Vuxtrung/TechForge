package com.swp391.techforge.entity;

public enum ContactStatus {
    PENDING("Đợi phản hồi"),
    REPLIED("Đã phản hồi"),
    HIDDEN("Đã ẩn");

    private final String label;

    ContactStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}