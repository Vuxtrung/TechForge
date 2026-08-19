package com.swp391.techforge.controller.api;

import com.swp391.techforge.dto.BuildPcProductDto;
import com.swp391.techforge.dto.BuildPcValidateRequest;
import com.swp391.techforge.dto.CompatibilityReport;
import com.swp391.techforge.entity.Product;
import com.swp391.techforge.service.buildpc.PcCompatibilityService;
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
    private final PcCompatibilityService compatibilityService;
    private final CategoryRepository categoryRepository;

    public BuildPcApiController(ProductService productService, PcCompatibilityService compatibilityService, CategoryRepository categoryRepository) {
        this.productService = productService;
        this.compatibilityService = compatibilityService;
        this.categoryRepository = categoryRepository;
    }

    /**
     * API lấy danh sách linh kiện thuộc một danh mục cụ thể (Ví dụ: CPU, VGA).
     * Phục vụ cho việc load danh sách sản phẩm khi người dùng click chọn trên giao diện Build PC.
     * 
     * @param categoryName Tên danh mục cần lấy sản phẩm (CPU, Mainboard...)
     * @param page Số trang hiện tại (mặc định 0)
     * @param size Số lượng sản phẩm mỗi trang (mặc định 20)
     * @param sort Cú pháp sắp xếp (mặc định theo giá tăng dần)
     * @return Danh sách các sản phẩm đã được map sang DTO (BuildPcProductDto)
     */
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

    /**
     * API kiểm tra tính tương thích của hệ thống máy tính.
     * Nhận vào ID của các linh kiện người dùng đã chọn, lấy thông tin chi tiết từ cơ sở dữ liệu
     * và đưa qua PcCompatibilityService để chạy thuật toán kiểm tra.
     * 
     * @param request Chứa ID của CPU, Mainboard, RAM, VGA, PSU
     * @return CompatibilityReport Báo cáo lỗi/cảnh báo và điện năng tiêu thụ dự kiến (JSON)
     */
    @PostMapping("/validate")
    public ResponseEntity<CompatibilityReport> validateBuild(@RequestBody BuildPcValidateRequest request) {
        Product cpu = request.getCpuId() != null ? getProductSafely(request.getCpuId()) : null;
        Product mainboard = request.getMainboardId() != null ? getProductSafely(request.getMainboardId()) : null;
        Product ram = request.getRamId() != null ? getProductSafely(request.getRamId()) : null;
        Product vga = request.getVgaId() != null ? getProductSafely(request.getVgaId()) : null;
        Product psu = request.getPsuId() != null ? getProductSafely(request.getPsuId()) : null;

        CompatibilityReport report = compatibilityService.checkCompatibility(cpu, mainboard, ram, vga, psu);
        return ResponseEntity.ok(report);
    }

    /**
     * Hàm phụ trợ: Lấy thông tin sản phẩm một cách an toàn.
     * Bắt lỗi nếu sản phẩm không tồn tại để tránh sập hệ thống (ném ra exception).
     * 
     * @param id ID của sản phẩm
     * @return Product hoặc null nếu không tìm thấy
     */
    private Product getProductSafely(Long id) {
        try {
            return productService.getById(id);
        } catch (Exception e) {
            return null;
        }
    }
}
