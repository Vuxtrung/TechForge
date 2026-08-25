package com.swp391.techforge.service;

import com.swp391.techforge.dto.BuildPcValidateRequest;
import com.swp391.techforge.dto.CompatibilityReport;
import com.swp391.techforge.dto.BuildPcProductDto;
import com.swp391.techforge.entity.Category;
import com.swp391.techforge.entity.Product;
import com.swp391.techforge.entity.component.*;
import com.swp391.techforge.repository.category.CategoryRepository;
import com.swp391.techforge.repository.product.ProductRepository;
import com.swp391.techforge.repository.component.*;
import com.swp391.techforge.service.buildpc.CompatibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PcBuilderService {

    private final CpuRepository cpuRepository;
    private final MainboardRepository mainboardRepository;
    private final RamRepository ramRepository;
    private final GpuRepository gpuRepository;
    private final PsuRepository psuRepository;
    private final CaseComponentRepository caseRepository;
    private final CoolerRepository coolerRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CompatibilityService compatibilityService;

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
            String cpuSocket = cpu.getSocket() != null ? cpu.getSocket().replace(",", "").trim() : null;
            String mbSocket = mainboard.getSocket() != null ? mainboard.getSocket().replace(",", "").trim() : null;
            if (cpuSocket != null && mbSocket != null && !cpuSocket.equalsIgnoreCase(mbSocket)) {
                report.addError("CPU socket (" + cpuSocket + ") không tương thích với Mainboard socket (" + mbSocket + ").");
            }
        }
        if (cpu != null && cpu.getTdpWatt() != null) {
            totalRequiredWattage += cpu.getTdpWatt();
        }

        // 2. RAM & Mainboard Compatibility
        if (ram != null && mainboard != null) {
            if (ram.getRamType() != null && mainboard.getRamType() != null && !ram.getRamType().name().equals(mainboard.getRamType().name())) {
                report.addError("Chuẩn RAM (" + ram.getRamType().name() + ") không được hỗ trợ bởi Mainboard (" + mainboard.getRamType().name() + ").");
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
                String cpuSocket = cpu.getSocket().replace(",", "").trim();
                if (!cooler.getSocketSupport().contains(cpuSocket)) {
                    report.addError("Tản nhiệt không hỗ trợ socket CPU " + cpuSocket + ".");
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

    public List<BuildPcProductDto> getComponentsByCategory(String categoryKey, String sortStr, int size, BuildPcValidateRequest selectedComponents) {
        Category.ComponentType componentType = mapKeyToComponentType(categoryKey);
        if (componentType == null) {
            return List.of();
        }

        List<Category> categories = categoryRepository.findAllByActiveTrueAndComponentTypeOrderByNameAsc(componentType);
        if (categories == null || categories.isEmpty()) {
            return List.of();
        }

        List<Long> categoryIds = categories.stream().map(Category::getCategoryId).collect(Collectors.toList());

        // Lấy tất cả active product trong danh mục này để làm ứng viên lọc (giới hạn 1000 để tránh lag nếu data quá lớn)
        PageRequest maxPageRequest = PageRequest.of(0, 1000);
        Page<Product> allProductsPage = productRepository.searchPublic(null, categoryIds, null, null, null, null, maxPageRequest);
        List<Long> candidateProductIds = allProductsPage.getContent().stream()
                .map(Product::getProductId)
                .collect(Collectors.toList());

        if (candidateProductIds.isEmpty()) {
            return List.of();
        }

        List<Long> compatibleProductIds = new ArrayList<>();

        if (selectedComponents == null) {
            compatibleProductIds.addAll(candidateProductIds);
        } else {
            Cpu cpu = selectedComponents.getCpuId() != null ? cpuRepository.findById(selectedComponents.getCpuId()).orElse(null) : null;
            Mainboard mainboard = selectedComponents.getMainboardId() != null ? mainboardRepository.findById(selectedComponents.getMainboardId()).orElse(null) : null;
            Ram ram = selectedComponents.getRamId() != null ? ramRepository.findById(selectedComponents.getRamId()).orElse(null) : null;
            Gpu gpu = selectedComponents.getVgaId() != null ? gpuRepository.findById(selectedComponents.getVgaId()).orElse(null) : null;
            Psu psu = selectedComponents.getPsuId() != null ? psuRepository.findById(selectedComponents.getPsuId()).orElse(null) : null;
            CaseComponent pcCase = selectedComponents.getCaseId() != null ? caseRepository.findById(selectedComponents.getCaseId()).orElse(null) : null;
            Cooler cooler = selectedComponents.getCoolerId() != null ? coolerRepository.findById(selectedComponents.getCoolerId()).orElse(null) : null;

            switch (componentType) {
                case CPU:
                    List<Cpu> candidateCpus = cpuRepository.findAllById(candidateProductIds);
                    for (Cpu candidate : candidateCpus) {
                        CompatibilityReport report = new CompatibilityReport();
                        compatibilityService.checkCpuMainboard(candidate, mainboard, report);
                        compatibilityService.checkCoolerCpu(cooler, candidate, report);
                        compatibilityService.checkPsuWattage(candidate, gpu, psu, report);
                        if (report.getErrors().isEmpty()) {
                            compatibleProductIds.add(candidate.getProductId());
                        }
                    }
                    break;
                case MAINBOARD:
                    List<Mainboard> candidateMbs = mainboardRepository.findAllById(candidateProductIds);
                    for (Mainboard candidate : candidateMbs) {
                        CompatibilityReport report = new CompatibilityReport();
                        compatibilityService.checkCpuMainboard(cpu, candidate, report);
                        compatibilityService.checkMainboardRam(candidate, ram, report);
                        compatibilityService.checkMainboardCase(candidate, pcCase, report);
                        if (report.getErrors().isEmpty()) {
                            compatibleProductIds.add(candidate.getProductId());
                        }
                    }
                    break;
                case RAM:
                    List<Ram> candidateRams = ramRepository.findAllById(candidateProductIds);
                    for (Ram candidate : candidateRams) {
                        CompatibilityReport report = new CompatibilityReport();
                        compatibilityService.checkMainboardRam(mainboard, candidate, report);
                        if (report.getErrors().isEmpty()) {
                            compatibleProductIds.add(candidate.getProductId());
                        }
                    }
                    break;
                case GPU:
                    List<Gpu> candidateGpus = gpuRepository.findAllById(candidateProductIds);
                    for (Gpu candidate : candidateGpus) {
                        CompatibilityReport report = new CompatibilityReport();
                        compatibilityService.checkGpuCase(candidate, pcCase, report);
                        compatibilityService.checkPsuWattage(cpu, candidate, psu, report);
                        if (report.getErrors().isEmpty()) {
                            compatibleProductIds.add(candidate.getProductId());
                        }
                    }
                    break;
                case PSU:
                    List<Psu> candidatePsus = psuRepository.findAllById(candidateProductIds);
                    for (Psu candidate : candidatePsus) {
                        CompatibilityReport report = new CompatibilityReport();
                        compatibilityService.checkPsuWattage(cpu, gpu, candidate, report);
                        if (report.getErrors().isEmpty()) {
                            compatibleProductIds.add(candidate.getProductId());
                        }
                    }
                    break;
                case CASE_TYPE:
                    List<CaseComponent> candidateCases = caseRepository.findAllById(candidateProductIds);
                    for (CaseComponent candidate : candidateCases) {
                        CompatibilityReport report = new CompatibilityReport();
                        compatibilityService.checkMainboardCase(mainboard, candidate, report);
                        compatibilityService.checkGpuCase(gpu, candidate, report);
                        // Bỏ qua checkCoolerCase cho tản nước (và cả khí) ở bước lọc theo yêu cầu
                        if (report.getErrors().isEmpty()) {
                            compatibleProductIds.add(candidate.getProductId());
                        }
                    }
                    break;
                case COOLER:
                    List<Cooler> candidateCoolers = coolerRepository.findAllById(candidateProductIds);
                    for (Cooler candidate : candidateCoolers) {
                        CompatibilityReport report = new CompatibilityReport();
                        compatibilityService.checkCoolerCpu(candidate, cpu, report);
                        // Bỏ qua checkCoolerCase ở đây
                        if (report.getErrors().isEmpty()) {
                            compatibleProductIds.add(candidate.getProductId());
                        }
                    }
                    break;
                case STORAGE:
                default:
                    compatibleProductIds.addAll(candidateProductIds);
                    break;
            }
        }

        if (compatibleProductIds.isEmpty()) {
            return List.of();
        }

        String[] sortParams = sortStr.split(",");
        Sort sort = Sort.by(Sort.Direction.fromString(sortParams[1]), sortParams[0]);
        PageRequest pageRequest = PageRequest.of(0, size, sort);

        Page<Product> finalProductPage = productRepository.searchPublicByProductIds(compatibleProductIds, pageRequest);

        return finalProductPage.getContent().stream()
                .map(BuildPcProductDto::new)
                .collect(Collectors.toList());
    }

    private Category.ComponentType mapKeyToComponentType(String key) {
        if (key == null) return null;
        switch (key.toUpperCase()) {
            case "CPU": return Category.ComponentType.CPU;
            case "MAINBOARD": return Category.ComponentType.MAINBOARD;
            case "RAM": return Category.ComponentType.RAM;
            case "VGA": return Category.ComponentType.GPU;
            case "NGUỒN": 
            case "NGUON": return Category.ComponentType.PSU;
            case "Ổ CỨNG":
            case "O CUNG": return Category.ComponentType.STORAGE;
            case "FAN TẢN NHIỆT":
            case "FAN TAN NHIET": return Category.ComponentType.COOLER;
            case "VỎ MÁY":
            case "VO MAY": return Category.ComponentType.CASE_TYPE;
            default: return null;
        }
    }
}
