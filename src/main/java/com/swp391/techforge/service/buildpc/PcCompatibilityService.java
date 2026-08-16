package com.swp391.techforge.service.buildpc;

import com.swp391.techforge.dto.CompatibilityReport;
import com.swp391.techforge.entity.Product;
import com.swp391.techforge.entity.ProductSpecification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PcCompatibilityService {

    /**
     * Hàm lấy giá trị của một thông số kỹ thuật (Spec) dựa trên khóa (specKey).
     */
    private String extractSpecValue(Product product, String specKey) {
        if (product == null || product.getSpecifications() == null) {
            return null;
        }
        for (ProductSpecification spec : product.getSpecifications()) {
            if (spec.getSpecKey().equalsIgnoreCase(specKey)) {
                return spec.getSpecValue().trim();
            }
        }
        return null;
    }

    /**
     * Hàm parse số từ chuỗi (Ví dụ: "65W" -> 65, "1000W" -> 1000)
     */
    private int parseIntValue(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            // Loại bỏ các ký tự không phải số
            String numStr = value.replaceAll("[^0-9]", "");
            if (numStr.isEmpty()) {
                return 0;
            }
            return Integer.parseInt(numStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Thuật toán chính: Kiểm tra sự tương thích giữa các linh kiện đã chọn
     */
    public CompatibilityReport checkCompatibility(Product cpu, Product mainboard, Product ram, Product vga, Product psu) {
        CompatibilityReport report = new CompatibilityReport();
        int totalWattage = 100; // Mặc định 100W cho Mainboard, Fans, Ổ cứng...

        // 1. Kiểm tra Socket (CPU & Mainboard)
        if (cpu != null && mainboard != null) {
            String cpuSocket = extractSpecValue(cpu, "Socket");
            String mainSocket = extractSpecValue(mainboard, "Socket");

            if (cpuSocket == null || mainSocket == null) {
                report.addWarning("Không thể kiểm tra Socket vì thiếu thông tin (spec_key = 'Socket').");
            } else if (!cpuSocket.equalsIgnoreCase(mainSocket)) {
                report.addError("Lỗi Tương Thích Socket: CPU dùng Socket " + cpuSocket + " nhưng Mainboard dùng Socket " + mainSocket + ".");
            }
        }

        // 2. Kiểm tra chuẩn RAM (RAM & Mainboard)
        if (ram != null && mainboard != null) {
            String ramType = extractSpecValue(ram, "RAM Type");
            String mainRamType = extractSpecValue(mainboard, "RAM Type");

            if (ramType == null || mainRamType == null) {
                report.addWarning("Không thể kiểm tra chuẩn RAM vì thiếu thông tin (spec_key = 'RAM Type').");
            } else if (!ramType.equalsIgnoreCase(mainRamType)) {
                report.addError("Lỗi Tương Thích RAM: Mainboard hỗ trợ " + mainRamType + " nhưng RAM bạn chọn là " + ramType + ".");
            }
        }

        // 3. Tính toán điện năng tiêu thụ (TDP)
        if (cpu != null) {
            int cpuTdp = parseIntValue(extractSpecValue(cpu, "TDP"));
            if (cpuTdp == 0) report.addWarning("CPU thiếu thông tin công suất (spec_key = 'TDP'). Tính toán có thể không chính xác.");
            totalWattage += cpuTdp;
        }

        if (vga != null) {
            int vgaTdp = parseIntValue(extractSpecValue(vga, "TDP"));
            if (vgaTdp == 0) report.addWarning("VGA thiếu thông tin công suất (spec_key = 'TDP'). Tính toán có thể không chính xác.");
            totalWattage += vgaTdp;
        }

        report.setEstimatedWattage(totalWattage);

        // 4. Kiểm tra công suất nguồn (PSU)
        if (psu != null) {
            int psuWattage = parseIntValue(extractSpecValue(psu, "Wattage"));
            if (psuWattage == 0) {
                report.addWarning("Nguồn (PSU) thiếu thông tin công suất thực (spec_key = 'Wattage').");
            } else {
                // Khuyến nghị nguồn lớn hơn công suất tiêu thụ tối thiểu 20-30%
                int recommendedWattage = (int) (totalWattage * 1.3);
                if (psuWattage < totalWattage) {
                    report.addError("Lỗi Nguồn (PSU): Công suất hệ thống yêu cầu (" + totalWattage + "W) vượt quá công suất nguồn bạn chọn (" + psuWattage + "W). Hệ thống sẽ không hoạt động!");
                } else if (psuWattage < recommendedWattage) {
                    report.addWarning("Cảnh báo Nguồn (PSU): Công suất nguồn (" + psuWattage + "W) hơi sát với công suất yêu cầu (" + totalWattage + "W). Bạn nên chọn nguồn có công suất tối thiểu " + recommendedWattage + "W để hệ thống ổn định lâu dài.");
                }
            }
        } else {
            report.addWarning("Bạn chưa chọn Nguồn (PSU). Công suất dự kiến hiện tại là " + totalWattage + "W.");
        }

        return report;
    }
}
