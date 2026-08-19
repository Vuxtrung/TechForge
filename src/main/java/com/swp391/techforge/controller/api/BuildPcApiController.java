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
import com.swp391.techforge.service.buildpc.CompatibilityService;
import com.swp391.techforge.service.product.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    private final ProductService productService;
    private final CompatibilityService compatibilityService;
    private final CategoryRepository categoryRepository;
    private final CpuRepository cpuRepository;
    private final MainboardRepository mainboardRepository;
    private final RamRepository ramRepository;
    private final GpuRepository gpuRepository;
    private final PsuRepository psuRepository;
    private final CaseComponentRepository caseComponentRepository;
    private final CoolerRepository coolerRepository;

    public BuildPcApiController(ProductService productService,
            CompatibilityService compatibilityService,
            CategoryRepository categoryRepository,
            CpuRepository cpuRepository,
            MainboardRepository mainboardRepository,
            RamRepository ramRepository,
            GpuRepository gpuRepository,
            PsuRepository psuRepository,
            CaseComponentRepository caseComponentRepository,
            CoolerRepository coolerRepository) {
        this.productService = productService;
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
                
        // Find category ID by name
        Long categoryId = null;
        Page<Category> catPage = categoryRepository.search(categoryName, null, true, PageRequest.of(0, 1));
        if (!catPage.isEmpty()) {
            categoryId = catPage.getContent().get(0).getCategoryId();
        } else {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
        
        // Fetch products
        Page<Product> productPage = productService.search("", categoryId, "ACTIVE", page, size, Sort.by(direction, sortParts[0]));
        
        List<BuildPcProductDto> dtoList = productPage.getContent().stream()
                .map(BuildPcProductDto::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
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