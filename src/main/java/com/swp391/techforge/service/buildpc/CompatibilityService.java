package com.swp391.techforge.service.buildpc;

import com.swp391.techforge.dto.CompatibilityReport;
import com.swp391.techforge.entity.component.CaseComponent;
import com.swp391.techforge.entity.component.Cooler;
import com.swp391.techforge.entity.component.Cpu;
import com.swp391.techforge.entity.component.Gpu;
import com.swp391.techforge.entity.component.Mainboard;
import com.swp391.techforge.entity.component.Psu;
import com.swp391.techforge.entity.component.Ram;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Kiểm tra tương thích linh kiện cho tính năng Build PC (F_20).
 * Rule hard-code trong {@link RuleCode}, KHÔNG lưu DB, luôn bật.
 * Mỗi method so sánh trực tiếp cột giữa 2 bảng con tương ứng.
 * Tất cả method đều null-safe: nếu 1 trong 2 linh kiện chưa được chọn
 * (null) thì coi như chưa đủ dữ liệu để check -> bỏ qua, không báo lỗi.
 */
@Service
public class CompatibilityService {

    // Công suất "nền" ước tính cho các linh kiện không tính riêng
    // (mainboard, ram, quạt case, ổ cứng...) khi tính tổng wattage hệ thống.
    private static final int BASE_SYSTEM_WATT = 100;
    // Biên an toàn khuyến nghị: PSU nên có công suất >= (tải ước tính) * 1.2
    private static final double PSU_SAFETY_MARGIN = 1.2;

    /**
     * Kiểm tra toàn bộ cấu hình đã chọn, gộp kết quả từ tất cả các cặp liên quan.
     * Component nào chưa chọn (null) thì các rule liên quan tới nó sẽ tự bỏ qua.
     */
    public CompatibilityReport checkFullBuild(Cpu cpu, Mainboard mainboard, Ram ram,
            Gpu gpu, Psu psu, CaseComponent caseComponent, Cooler cooler) {
        CompatibilityReport report = new CompatibilityReport();

        checkCpuMainboard(cpu, mainboard, report);
        checkMainboardRam(mainboard, ram, report);
        checkMainboardCase(mainboard, caseComponent, report);
        checkGpuCase(gpu, caseComponent, report);
        checkCoolerCase(cooler, caseComponent, report);
        checkCoolerCpu(cooler, cpu, report);
        checkPsuWattage(cpu, gpu, psu, report);

        report.setEstimatedWattage(estimateWattage(cpu, gpu));
        return report;
    }

    // ==== CPU <-> Mainboard: socket phải khớp ====
    public void checkCpuMainboard(Cpu cpu, Mainboard mainboard, CompatibilityReport report) {
        if (cpu == null || mainboard == null) {
            return;
        }
        if (cpu.getSocket() == null || mainboard.getSocket() == null) {
            return;
        }
        String cpuSocket = cpu.getSocket().replace(",", "").trim();
        String mbSocket = mainboard.getSocket().replace(",", "").trim();
        if (!cpuSocket.equalsIgnoreCase(mbSocket)) {
            report.addError(RuleCode.CPU_SOCKET_MB.getMessage()
                    + " (CPU: " + cpu.getSocket() + ", Mainboard: " + mainboard.getSocket() + ")");
        }
    }

    // ==== Mainboard <-> RAM: loại RAM phải khớp + không vượt slot/dung lượng tối đa ====
    public void checkMainboardRam(Mainboard mainboard, Ram ram, CompatibilityReport report) {
        if (mainboard == null || ram == null) {
            return;
        }
        if (mainboard.getRamType() != null && ram.getRamType() != null
                && !mainboard.getRamType().name().equals(ram.getRamType().name())) {
            report.addError(RuleCode.MB_RAM_TYPE.getMessage()
                    + " (Mainboard: " + mainboard.getRamType() + ", RAM: " + ram.getRamType() + ")");
        }
        if (mainboard.getRamSlots() != null && ram.getModules() != null
                && ram.getModules() > mainboard.getRamSlots()) {
            report.addError(RuleCode.RAM_MODULES_EXCEED_SLOTS.getMessage()
                    + " (RAM: " + ram.getModules() + " thanh, Mainboard: " + mainboard.getRamSlots() + " khe)");
        }
        if (mainboard.getMaxRamGb() != null && ram.getCapacityGb() != null && ram.getModules() != null) {
            int totalCapacity = ram.getCapacityGb() * ram.getModules();
            if (totalCapacity > mainboard.getMaxRamGb()) {
                report.addError(RuleCode.RAM_CAPACITY_EXCEEDS_MB.getMessage()
                        + " (RAM: " + totalCapacity + "GB, Mainboard tối đa: " + mainboard.getMaxRamGb() + "GB)");
            }
        }
    }

    // ==== Mainboard <-> Case: form factor mainboard phải nằm trong danh sách case hỗ trợ ====
    public void checkMainboardCase(Mainboard mainboard, CaseComponent caseComponent, CompatibilityReport report) {
        if (mainboard == null || caseComponent == null) {
            return;
        }
        if (mainboard.getFormFactor() == null || caseComponent.getFormFactorSupport() == null) {
            return;
        }
        List<String> supported = Arrays.asList(caseComponent.getFormFactorSupport().split(","));
        boolean ok = supported.stream()
                .anyMatch(ff -> ff.trim().equalsIgnoreCase(mainboard.getFormFactor().name()));
        if (!ok) {
            report.addError(RuleCode.MB_FORM_FACTOR_CASE.getMessage()
                    + " (Mainboard: " + mainboard.getFormFactor()
                    + ", Case hỗ trợ: " + caseComponent.getFormFactorSupport() + ")");
        }
    }

    // ==== GPU <-> Case: chiều dài GPU không vượt quá không gian case ====
    public void checkGpuCase(Gpu gpu, CaseComponent caseComponent, CompatibilityReport report) {
        if (gpu == null || caseComponent == null) {
            return;
        }
        if (gpu.getLengthMm() == null || caseComponent.getMaxGpuLengthMm() == null) {
            return;
        }
        if (gpu.getLengthMm() > caseComponent.getMaxGpuLengthMm()) {
            report.addError(RuleCode.GPU_LENGTH_CASE.getMessage()
                    + " (GPU: " + gpu.getLengthMm() + "mm, Case tối đa: " + caseComponent.getMaxGpuLengthMm() + "mm)");
        }
    }

    // ==== Cooler <-> Case: chiều cao (Air) hoặc radiator (AIO) không vượt quá case ====
    public void checkCoolerCase(Cooler cooler, CaseComponent caseComponent, CompatibilityReport report) {
        if (cooler == null || caseComponent == null || cooler.getCoolerType() == null) {
            return;
        }
        if (cooler.getCoolerType() == Cooler.CoolerType.AIR) {
            if (cooler.getHeightMm() != null && caseComponent.getMaxCoolerHeightMm() != null
                    && cooler.getHeightMm() > caseComponent.getMaxCoolerHeightMm()) {
                report.addError(RuleCode.COOLER_HEIGHT_CASE.getMessage()
                        + " (Tản: " + cooler.getHeightMm() + "mm, Case tối đa: "
                        + caseComponent.getMaxCoolerHeightMm() + "mm)");
            }
        } else if (cooler.getCoolerType() == Cooler.CoolerType.AIO) {
            if (cooler.getRadiatorSizeMm() != null && caseComponent.getMaxRadiatorMm() != null
                    && cooler.getRadiatorSizeMm() > caseComponent.getMaxRadiatorMm()) {
                report.addError(RuleCode.COOLER_RADIATOR_CASE.getMessage()
                        + " (Radiator: " + cooler.getRadiatorSizeMm() + "mm, Case tối đa: "
                        + caseComponent.getMaxRadiatorMm() + "mm)");
            }
        }
    }

    // ==== Cooler <-> CPU: socket_support (nếu có khai báo) phải chứa socket của CPU ====
    public void checkCoolerCpu(Cooler cooler, Cpu cpu, CompatibilityReport report) {
        if (cooler == null || cpu == null) {
            return;
        }
        // socketSupport có thể null (nhà sản xuất không khai báo chi tiết) -> bỏ qua,
        // không coi là lỗi vì thiếu dữ liệu không có nghĩa là không tương thích.
        if (cooler.getSocketSupport() == null || cpu.getSocket() == null) {
            return;
        }
        String cpuSocket = cpu.getSocket().replace(",", "").trim();
        boolean ok = Arrays.stream(cooler.getSocketSupport().split(","))
                .anyMatch(s -> s.trim().equalsIgnoreCase(cpuSocket));
        if (!ok) {
            report.addError(RuleCode.COOLER_SOCKET_CPU.getMessage()
                    + " (CPU: " + cpu.getSocket() + ", Tản hỗ trợ: " + cooler.getSocketSupport() + ")");
        }
    }

    // ==== PSU: công suất phải đủ cho CPU + GPU (kèm biên an toàn) ====
    public void checkPsuWattage(Cpu cpu, Gpu gpu, Psu psu, CompatibilityReport report) {
        if (psu == null || psu.getWattage() == null) {
            return;
        }
        int estimated = estimateWattage(cpu, gpu);
        int recommended = (int) Math.ceil(estimated * PSU_SAFETY_MARGIN);
        if (psu.getWattage() < recommended) {
            report.addError(RuleCode.PSU_WATTAGE.getMessage()
                    + " (PSU: " + psu.getWattage() + "W, khuyến nghị tối thiểu: " + recommended + "W)");
        }
    }

    // Ước tính tổng công suất hệ thống: TDP CPU + công suất khuyến nghị GPU
    // (đã bao gồm phần của GPU) + mức nền cho các linh kiện khác.
    private int estimateWattage(Cpu cpu, Gpu gpu) {
        int total = BASE_SYSTEM_WATT;
        if (cpu != null && cpu.getTdpWatt() != null) {
            total += cpu.getTdpWatt();
        }
        if (gpu != null && gpu.getRecommendedPsuWatt() != null) {
            // recommended_psu_watt của GPU thường đã là khuyến nghị PSU tổng
            // cho hệ thống có GPU đó, nên lấy giá trị lớn hơn thay vì cộng dồn
            // để tránh thổi phồng công suất yêu cầu.
            total = Math.max(total, gpu.getRecommendedPsuWatt());
        }
        return total;
    }
}