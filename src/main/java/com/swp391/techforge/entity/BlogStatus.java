package com.swp391.techforge.entity;

/**
 * Trạng thái xuất bản và kiểm duyệt của bài viết blog
 */
public enum BlogStatus {
    DRAFT("Bản nháp", "bg-warning text-dark"),
    PUBLISHED("Đã xuất bản", "bg-success text-white"),
    HIDDEN("Tạm ẩn", "bg-secondary text-white"),
    REJECTED("Bị từ chối", "bg-danger text-white");

    private final String displayName;
    private final String badgeClass;

    BlogStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
