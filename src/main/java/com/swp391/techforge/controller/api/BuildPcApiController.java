package com.swp391.techforge.controller.api;

import com.swp391.techforge.dto.BuildPcProductDto;
import com.swp391.techforge.dto.BuildPcValidateRequest;
import com.swp391.techforge.dto.CompatibilityReport;
import com.swp391.techforge.entity.Product;
import com.swp391.techforge.entity.component.CaseComponent;
import com.swp391.techforge.entity.component.Cooler;
import com.swp391.techforge.entity.component.Cpu;
import com.swp391.techforge.entity.component.Gpu;
import com.swp391.techforge.entity.component.Mainboard;
import com.swp391.techforge.entity.component.Psu;
import com.swp391.techforge.entity.component.Ram;
import com.swp391.techforge.repository.component.CaseComponentRepository;
import com.swp391.techforge.repository.component.CoolerRepository;
import com.swp391.techforge.repository.component.CpuRepository;
import com.swp391.techforge.repository.component.GpuRepository;
import com.swp391.techforge.repository.component.MainboardRepository;
import com.swp391.techforge.repository.component.PsuRepository;
import com.swp391.techforge.repository.component.RamRepository;
import com.swp391.techforge.repository.product.ProductRepository;
import com.swp391.techforge.service.buildpc.CompatibilityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.swp391.techforge.repository.category.CategoryRepository;
import com.swp391.techforge.entity.Category;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/buildpc")
public class BuildPcApiController {

    private final ProductRepository productRepository;
    private final CompatibilityService compatibilityService;
    private final CategoryRepository categoryRepository;
    private final CpuRepository cpuRepository;
    private final MainboardRepository mainboardRepository;
    private final RamRepository ramRepository;
    private final GpuRepository gpuRepository;
    private final PsuRepository psuRepository;
    private final CaseComponentRepository caseComponentRepository;
    private final CoolerRepository coolerRepository;

    public BuildPcApiController(ProductRepository productRepository,
            CompatibilityService compatibilityService,
            CategoryRepository categoryRepository,
            CpuRepository cpuRepository,
            MainboardRepository mainboardRepository,
            RamRepository ramRepository,
            GpuRepository gpuRepository,
            PsuRepository psuRepository,
            CaseComponentRepository caseComponentRepository,
            CoolerRepository coolerRepository) {
        this.productRepository = productRepository;
        this.compatibilityService = compatibilityService;
        this.categoryRepository = categoryRepository;
        this.cpuRepository = cpuRepository;
        this.mainboardRepository = mainboardRepository;
        this.ramRepository = ramRepository;
        this.gpuRepository = gpuRepository;
        this.psuRepository = psuRepository;
        this.caseComponentRepository = caseComponentRepository;
        this.coolerRepository = coolerRepository;
    }

    @GetMapping("/components")
    public ResponseEntity<List<BuildPcProductDto>> 
            getComponents(
            @RequestParam String categoryName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "basePrice,asc") String sort) {
        
        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc") 
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        // Tìm category theo tên: ưu tiên khớp CHÍNH XÁC (không phân biệt hoa
        // thường) trước, chỉ fallback sang LIKE nếu không có khớp chính xác nào
        // -> tránh trường hợp khớp nhầm category khác chỉ vì tên chứa substring
        // (VD categoryName="CPU" khớp nhầm "CPU Cooler" nếu dùng LIKE trước).
        List<Category> exactMatches = categoryRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .filter(c -> c.getName().equalsIgnoreCase(categoryName))
                .collect(Collectors.toList());

        Long categoryId;
        if (!exactMatches.isEmpty()) {
            categoryId = exactMatches.get(0).getCategoryId();
        } else {
                        Category.ComponentType componentType = resolveComponentType(categoryName);
                        List<Category> componentCategories = componentType == null
                                        ? java.util.Collections.emptyList()
                                        : categoryRepository.findAllByActiveTrueAndComponentTypeOrderByNameAsc(componentType);
                        if (!componentCategories.isEmpty()) {
                                categoryId = componentCategories.get(0).getCategoryId();
                        } else {
                                Page<Category> catPage = categoryRepository.search(categoryName, null, true, PageRequest.of(0, 1));
                                if (catPage.isEmpty()) {
                                        return ResponseEntity.ok(java.util.Collections.emptyList());
                                }
                                categoryId = catPage.getContent().get(0).getCategoryId();
            }
        }

        // Lấy luôn category con cháu (đệ quy) của category vừa tìm được, giống
        // cách trang /products đang làm (searchPublic + findSelfAndDescendantIds)
        // -> sản phẩm được gán ở category cha hay category con đều tìm thấy.
        List<Long> categoryIds = categoryRepository.findSelfAndDescendantIds(categoryId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));
        Category.ComponentType componentType = resolveComponentType(categoryName);
        Page<Product> productPage;
        if (componentType != null) {
            List<Long> componentProductIds = findComponentProductIds(componentType);
            productPage = componentProductIds.isEmpty()
                    ? Page.empty(pageable)
                    : productRepository.searchPublicByProductIds(componentProductIds, pageable);
        } else {
            productPage = productRepository.searchPublic(
                    "", categoryIds, null, null, null, null, pageable);
        }

        List<BuildPcProductDto> dtoList = productPage.getContent().stream()
                .map(BuildPcProductDto::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

        private Category.ComponentType resolveComponentType(String categoryName) {
                return switch (categoryName.trim().toUpperCase()) {
                        case "CPU" -> Category.ComponentType.CPU;
                        case "MAINBOARD" -> Category.ComponentType.MAINBOARD;
                        case "RAM" -> Category.ComponentType.RAM;
                        case "VGA" -> Category.ComponentType.GPU;
                        case "NGUỒN" -> Category.ComponentType.PSU;
                        case "Ổ CỨNG" -> Category.ComponentType.STORAGE;
                        case "FAN TẢN NHIỆT" -> Category.ComponentType.COOLER;
                        case "VỎ MÁY" -> Category.ComponentType.CASE_TYPE;
                        default -> null;
                };
        }

        private List<Long> findComponentProductIds(Category.ComponentType componentType) {
                return switch (componentType) {
                        case CPU -> cpuRepository.findAll().stream().map(Cpu::getProductId).toList();
                        case MAINBOARD -> mainboardRepository.findAll().stream().map(Mainboard::getProductId).toList();
                        case RAM -> ramRepository.findAll().stream().map(Ram::getProductId).toList();
                        case GPU -> gpuRepository.findAll().stream().map(Gpu::getProductId).toList();
                        case PSU -> psuRepository.findAll().stream().map(Psu::getProductId).toList();
                        case CASE_TYPE -> caseComponentRepository.findAll().stream().map(CaseComponent::getProductId).toList();
                        case COOLER -> coolerRepository.findAll().stream().map(Cooler::getProductId).toList();
                        case STORAGE -> java.util.Collections.emptyList();
                        case NONE -> java.util.Collections.emptyList();
                };
        }

    @PostMapping("/validate")
    public ResponseEntity<CompatibilityReport> validateBuild(@RequestBody BuildPcValidateRequest request) {
        Cpu cpu = request.getCpuId() != null ? cpuRepository.findById(request.getCpuId()).orElse(null) : null;
        Mainboard mainboard = request.getMainboardId() != null
                ? mainboardRepository.findById(request.getMainboardId()).orElse(null) : null;
        Ram ram = request.getRamId() != null ? ramRepository.findById(request.getRamId()).orElse(null) : null;
        Gpu gpu = request.getVgaId() != null ? gpuRepository.findById(request.getVgaId()).orElse(null) : null;
        Psu psu = request.getPsuId() != null ? psuRepository.findById(request.getPsuId()).orElse(null) : null;
        CaseComponent caseComponent = request.getCaseId() != null
                ? caseComponentRepository.findById(request.getCaseId()).orElse(null) : null;
        Cooler cooler = request.getCoolerId() != null
                ? coolerRepository.findById(request.getCoolerId()).orElse(null) : null;
        // Lưu ý: storageId không tham gia compatibility check (chưa có rule nào
        // liên quan tới ổ cứng), nên không cần load ở đây.

        CompatibilityReport report = compatibilityService.checkFullBuild(
                cpu, mainboard, ram, gpu, psu, caseComponent, cooler);
        return ResponseEntity.ok(report);
    }
}