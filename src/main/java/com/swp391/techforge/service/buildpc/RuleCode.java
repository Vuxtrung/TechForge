package com.swp391.techforge.service.buildpc;

/**
 * Danh sách rule tương thích linh kiện PC — hard-code (KHÔNG lưu DB).
 * Mỗi rule luôn bật, không có cơ chế toggle/admin quản lý.
 * message là mặc định; CompatibilityService có thể nối thêm chi tiết
 * (VD số liệu thực tế) khi add vào CompatibilityReport.
 */
public enum RuleCode {

    CPU_SOCKET_MB("Socket CPU không khớp với Mainboard."),
    MB_RAM_TYPE("Loại RAM không khớp với Mainboard (DDR4/DDR5)."),
    RAM_CAPACITY_EXCEEDS_MB("Tổng dung lượng RAM vượt quá mức Mainboard hỗ trợ."),
    RAM_MODULES_EXCEED_SLOTS("Số thanh RAM vượt quá số khe cắm trên Mainboard."),
    MB_FORM_FACTOR_CASE("Case không hỗ trợ form factor của Mainboard."),
    GPU_LENGTH_CASE("Chiều dài GPU vượt quá không gian tối đa của Case."),
    COOLER_HEIGHT_CASE("Chiều cao tản khí vượt quá giới hạn của Case."),
    COOLER_RADIATOR_CASE("Kích thước radiator (AIO) vượt quá giới hạn của Case."),
    COOLER_SOCKET_CPU("Tản nhiệt không hỗ trợ socket của CPU."),
    PSU_WATTAGE("Công suất nguồn (PSU) không đủ đáp ứng công suất ước tính của hệ thống.");

    private final String message;

    RuleCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}