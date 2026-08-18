package com.swp391.techforge.util;

import org.springframework.data.domain.Sort;

public final class SortUtil {

    private SortUtil() {
    }

    public static Sort parse(String value, String defaultField, String defaultDirection) {
        String sortValue = value == null || value.isBlank()
                ? defaultField + "," + defaultDirection
                : value;
        String[] parts = sortValue.split(",", 2);
        Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, parts[0]);
    }
}