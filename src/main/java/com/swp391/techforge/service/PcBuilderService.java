package com.swp391.techforge.service;

import com.swp391.techforge.dto.BuildPcValidateRequest;
import com.swp391.techforge.dto.CompatibilityReport;
import com.swp391.techforge.entity.component.*;
import com.swp391.techforge.repository.component.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PcBuilderService {

    private final CpuRepository cpuRepository;
    private final MainboardRepository mainboardRepository;
    private final RamRepository ramRepository;
    private final GpuRepository gpuRepository; // the request calls it vgaId
    private final PsuRepository psuRepository;
    private final CaseComponentRepository caseRepository;
    private final CoolerRepository coolerRepository;

    public CompatibilityReport checkCompatibility(BuildPcValidateRequest request) {
        CompatibilityReport report = new CompatibilityReport();

        Cpu cpu = request.getCpuId() != null ? cpuRepository.findById(request.getCpuId()).orElse(null) : null;
        Mainboard mainboard = request.getMainboardId() != null ? mainboardRepository.findById(request.getMainboardId()).orElse(null) : null;
        Ram ram = request.getRamId() != null ? ramRepository.findById(request.getRamId()).orElse(null) : null;
        Gpu gpu = request.getVgaId() != null ? gpuRepository.findById(request.getVgaId()).orElse(null) : null;
        Psu psu = request.getPsuId() != null ? psuRepository.findById(request.getPsuId()).orElse(null) : null;
        CaseComponent pcCase = request.getCaseId() != null ? caseRepository.findById(request.getCaseId()).orElse(null) : null;
        Cooler cooler = request.getCoolerId() != null ? coolerRepository.findById(request.getCoolerId()).orElse(null) : null;

        int totalRequiredWattage = 0;

        // 1. CPU & Mainboard Compatibility
        if (cpu != null && mainboard != null) {
            if (cpu.getSocket() != null && mainboard.getSocket() != null && !cpu.getSocket().equalsIgnoreCase(mainboard.getSocket())) {
                report.addError("CPU socket (" + cpu.getSocket() + ") không tương thích với Mainboard socket (" + mainboard.getSocket() + ").");
            }
        }
        if (cpu != null && cpu.getTdpWatt() != null) {
            totalRequiredWattage += cpu.getTdpWatt();
        }

        // 2. RAM & Mainboard Compatibility
        if (ram != null && mainboard != null) {
            if (ram.getRamType() != null && mainboard.getRamType() != null && !ram.getRamType().equals(mainboard.getRamType())) {
                report.addError("Chuẩn RAM (" + ram.getRamType() + ") không được hỗ trợ bởi Mainboard (" + mainboard.getRamType() + ").");
            }
        }

        // 3. Case & Mainboard Compatibility
        if (pcCase != null && mainboard != null) {
            if (pcCase.getFormFactorSupport() != null && mainboard.getFormFactor() != null) {
                if (!pcCase.getFormFactorSupport().contains(mainboard.getFormFactor().name())) {
                    report.addError("Case không hỗ trợ kích thước Mainboard " + mainboard.getFormFactor() + ".");
                }
            }
        }

        // 4. GPU & Case Compatibility
        if (gpu != null && pcCase != null) {
            if (gpu.getLengthMm() != null && pcCase.getMaxGpuLengthMm() != null) {
                if (gpu.getLengthMm() > pcCase.getMaxGpuLengthMm()) {
                    report.addError("Chiều dài VGA (" + gpu.getLengthMm() + "mm) vượt quá kích thước Case hỗ trợ (" + pcCase.getMaxGpuLengthMm() + "mm).");
                }
            }
        }
        if (gpu != null && gpu.getRecommendedPsuWatt() != null) {
            // Usually, GPU recommended wattage already accounts for the whole system, but to be safe we can use it directly or add
            // We'll use the recommended wattage of GPU + a bit of buffer if needed.
            // But let's just sum CPU TDP + GPU Recommended for an aggressive estimate, or just take the max.
            // Better to sum CPU TDP + some fixed amount for GPU. For simplicity, let's just add them.
            totalRequiredWattage += gpu.getRecommendedPsuWatt();
        }

        // 5. Cooler & Case / CPU Compatibility
        if (cooler != null) {
            if (cpu != null && cooler.getSocketSupport() != null && cpu.getSocket() != null) {
                if (!cooler.getSocketSupport().contains(cpu.getSocket())) {
                    report.addError("Tản nhiệt không hỗ trợ socket CPU " + cpu.getSocket() + ".");
                }
            }

            if (pcCase != null) {
                if (cooler.getCoolerType() == Cooler.CoolerType.AIR) {
                    if (cooler.getHeightMm() != null && pcCase.getMaxCoolerHeightMm() != null) {
                        if (cooler.getHeightMm() > pcCase.getMaxCoolerHeightMm()) {
                            report.addError("Chiều cao tản nhiệt khí (" + cooler.getHeightMm() + "mm) vượt quá khả năng hỗ trợ của Case (" + pcCase.getMaxCoolerHeightMm() + "mm).");
                        }
                    }
                } else if (cooler.getCoolerType() == Cooler.CoolerType.AIO) {
                    if (cooler.getRadiatorSizeMm() != null && pcCase.getMaxRadiatorMm() != null) {
                        if (cooler.getRadiatorSizeMm() > pcCase.getMaxRadiatorMm()) {
                            report.addError("Kích thước Radiator của tản nước (" + cooler.getRadiatorSizeMm() + "mm) vượt quá khả năng hỗ trợ của Case (" + pcCase.getMaxRadiatorMm() + "mm).");
                        }
                    }
                }
            }
        }

        // 6. PSU check
        report.setEstimatedWattage(totalRequiredWattage);
        if (psu != null) {
            if (psu.getWattage() != null) {
                if (psu.getWattage() < totalRequiredWattage) {
                    report.addError("Công suất nguồn (" + psu.getWattage() + "W) thấp hơn mức yêu cầu hệ thống (" + totalRequiredWattage + "W).");
                } else if (psu.getWattage() < totalRequiredWattage + 100) {
                    report.addWarning("Công suất nguồn (" + psu.getWattage() + "W) hoạt động sát với mức yêu cầu hệ thống. Nên cân nhắc nâng cấp để an toàn.");
                }
            }
        }

        return report;
    }
}
