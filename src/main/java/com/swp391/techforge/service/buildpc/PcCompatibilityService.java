package com.swp391.techforge.service.buildpc;

import com.swp391.techforge.dto.CompatibilityReport;
import com.swp391.techforge.entity.Product;
import com.swp391.techforge.entity.ProductSpecification;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PcCompatibilityService {

    private final CompatibilityRuleService ruleService;

    public PcCompatibilityService(CompatibilityRuleService ruleService) {
        this.ruleService = ruleService;
    }

    /**
     * Hàm lấy giá trị của một thông số kỹ thuật (Specification) cụ thể của sản phẩm.
     * Ví dụ: truyền vào sản phẩm CPU và từ khóa "Socket", hàm sẽ tìm trong danh sách thông số của CPU và trả về "LGA1700".
     * 
     * @param product Sản phẩm cần kiểm tra thông số (CPU, Mainboard, RAM...)
     * @param specKey Từ khóa thông số cần lấy (VD: "Socket", "TDP", "RAM Type")
     * @return Giá trị của thông số hoặc null nếu không tìm thấy
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
     * Hàm trích xuất phần số từ một chuỗi.
     * Sử dụng để tính toán điện năng tiêu thụ khi các thông số thường có chứa chữ cái (Ví dụ: "65W" -> 65, "1000W" -> 1000).
     * 
     * @param value Chuỗi chứa cả số và chữ
     * @return Giá trị số nguyên sau khi đã lọc bỏ chữ
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
     * Thuật toán chính: Kiểm tra sự tương thích của toàn bộ hệ thống PC sử dụng Rule Engine (SpEL).
     * Hàm sẽ lấy tất cả các quy tắc đã lưu trong cơ sở dữ liệu và đánh giá xem tổ hợp linh kiện hiện tại có thỏa mãn không.
     * 
     * @param cpu Linh kiện CPU đã chọn
     * @param mainboard Linh kiện Bo mạch chủ đã chọn
     * @param ram Linh kiện RAM đã chọn
     * @param vga Linh kiện Card màn hình đã chọn
     * @param psu Linh kiện Nguồn máy tính đã chọn
     * @return CompatibilityReport (Báo cáo tương thích chứa danh sách các lỗi và cảnh báo nếu có vi phạm)
     */
    public CompatibilityReport checkCompatibility(Product cpu, Product mainboard, Product ram, Product vga, Product psu) {
        CompatibilityReport report = new CompatibilityReport();
        
        // Tính toán Wattage
        int totalWattage = 100;
        if (cpu != null) totalWattage += parseIntValue(extractSpecValue(cpu, "TDP"));
        if (vga != null) totalWattage += parseIntValue(extractSpecValue(vga, "TDP"));
        report.setEstimatedWattage(totalWattage);

        // Load rules
        List<com.swp391.techforge.entity.CompatibilityRule> activeRules = ruleService.getActiveRules();
        if (activeRules == null || activeRules.isEmpty()) {
            return report; // Nếu chưa có rule nào, mặc định là hợp lệ (không có lỗi/cảnh báo)
        }

        org.springframework.expression.ExpressionParser parser = new org.springframework.expression.spel.standard.SpelExpressionParser();
        org.springframework.expression.spel.support.StandardEvaluationContext context = new org.springframework.expression.spel.support.StandardEvaluationContext();

        // Populate context variables
        if (cpu != null) {
            context.setVariable("CPU_Socket", extractSpecValue(cpu, "Socket"));
            context.setVariable("CPU_TDP", parseIntValue(extractSpecValue(cpu, "TDP")));
        }
        if (mainboard != null) {
            context.setVariable("Mainboard_Socket", extractSpecValue(mainboard, "Socket"));
            context.setVariable("Mainboard_RAM_Type", extractSpecValue(mainboard, "RAM Type"));
        }
        if (ram != null) {
            context.setVariable("RAM_Type", extractSpecValue(ram, "RAM Type"));
        }
        if (vga != null) {
            context.setVariable("VGA_TDP", parseIntValue(extractSpecValue(vga, "TDP")));
        }
        if (psu != null) {
            context.setVariable("PSU_Wattage", parseIntValue(extractSpecValue(psu, "Wattage")));
        }
        context.setVariable("Total_Wattage", totalWattage);

        // Evaluate rules
        for (com.swp391.techforge.entity.CompatibilityRule rule : activeRules) {
            try {
                Boolean result = parser.parseExpression(rule.getExpression()).getValue(context, Boolean.class);
                if (result != null && !result) {
                    if ("ERROR".equalsIgnoreCase(rule.getSeverity())) {
                        report.addError(rule.getMessage());
                    } else {
                        report.addWarning(rule.getMessage());
                    }
                }
            } catch (Exception e) {
                // Ignore evaluation errors (e.g. variable not found because product not selected)
                // In production, we might want to log this or handle it more gracefully
            }
        }

        return report;
    }
}
