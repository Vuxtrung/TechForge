package com.swp391.techforge.controller.admin;

import com.swp391.techforge.entity.Category;
import com.swp391.techforge.service.category.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
public class CategoryApiController {

    private final CategoryService categoryService;

    public CategoryApiController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/{id}/specs")
    public ResponseEntity<List<String>> getRequiredSpecsForCategory(@PathVariable Long id) {
        try {
            Category category = categoryService.getById(id);
            String categoryName = category.getName().toLowerCase();
            List<String> specs = new ArrayList<>();

            if (categoryName.contains("cpu") || categoryName.contains("vi xử lý")) {
                specs.add("Socket");
                specs.add("TDP");
            } else if (categoryName.contains("main") || categoryName.contains("bo mạch")) {
                specs.add("Socket");
                specs.add("RAM Type");
            } else if (categoryName.contains("ram") || categoryName.contains("bộ nhớ")) {
                specs.add("RAM Type");
            } else if (categoryName.contains("vga") || categoryName.contains("card màn hình")) {
                specs.add("TDP");
            } else if (categoryName.contains("psu") || categoryName.contains("nguồn")) {
                specs.add("Wattage");
            }

            return ResponseEntity.ok(specs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/testdump")
    public ResponseEntity<List<String>> dumpCategories() {
        return ResponseEntity.ok(categoryService.findAllActive().stream().map(Category::getName).toList());
    }
}
