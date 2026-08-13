package com.swp391.techforge.controller.admin;

import com.swp391.techforge.entity.Category;
import com.swp391.techforge.service.category.CategoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String active,
            @RequestParam(defaultValue = "name,asc") String sort,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        Page<Category> categoryPage = categoryService.search(
                keyword, type, active, page, 10, Sort.by(direction, sortParts[0]));

        model.addAttribute("categoryPage", categoryPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("type", type);
        model.addAttribute("active", active);
        model.addAttribute("sort", sort);
        return "admin/category-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("category", new Category());
        model.addAttribute("parentCategories", categoryService.findAllActive());
        return "admin/category-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("category", categoryService.getById(id));
        model.addAttribute("parentCategories", categoryService.findAllActive());
        return "admin/category-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("category") Category category,
                          BindingResult result, Model model,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("parentCategories", categoryService.findAllActive());
            return "admin/category-form";
        }
        try {
            categoryService.create(category);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("parentCategories", categoryService.findAllActive());
            return "admin/category-form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Thêm danh mục thành công.");
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("category") Category category,
                          BindingResult result, Model model,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("parentCategories", categoryService.findAllActive());
            return "admin/category-form";
        }
        try {
            categoryService.update(id, category);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("parentCategories", categoryService.findAllActive());
            return "admin/category-form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật danh mục thành công.");
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/toggle")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        categoryService.toggleActive(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái danh mục.");
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa danh mục.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/categories";
    }
}