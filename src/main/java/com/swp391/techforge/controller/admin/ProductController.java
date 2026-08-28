package com.swp391.techforge.controller.admin;

import com.swp391.techforge.dto.product.ComponentSpecRequest;
import com.swp391.techforge.entity.Product;
import com.swp391.techforge.service.category.CategoryService;
import com.swp391.techforge.service.product.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/products")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public ProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @InitBinder("spec")
    public void initBinder(WebDataBinder binder) {
        binder.setFieldDefaultPrefix("spec.");
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "name,asc") String sort,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        Page<Product> productPage = productService.search(
                keyword, categoryId, status, page, 10, Sort.by(direction, sortParts[0]));

        model.addAttribute("productPage", productPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("status", status);
        model.addAttribute("sort", sort);
        model.addAttribute("categories", categoryService.findAllActive());
        return "admin/product-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("spec", new ComponentSpecRequest());
        model.addAttribute("categories", categoryService.findAllChild());
        return "admin/product-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Product product = productService.getById(id);
        model.addAttribute("product", product);
        model.addAttribute("spec", productService.getSpecRequestForEdit(product));
        model.addAttribute("categories", categoryService.findAllActive());
        return "admin/product-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("product") Product product,
            BindingResult result, Model model,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @ModelAttribute("spec") ComponentSpecRequest specRequest,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("spec", specRequest);
            model.addAttribute("categories", categoryService.findAllActive());
            return "admin/product-form";
        }
        try {
            productService.create(product, imageFile, specRequest);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("spec", specRequest);
            model.addAttribute("categories", categoryService.findAllActive());
            return "admin/product-form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Thêm sản phẩm thành công.");
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
            @Valid @ModelAttribute("product") Product product,
            BindingResult result, Model model,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @ModelAttribute("spec") ComponentSpecRequest specRequest,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("spec", specRequest);
            model.addAttribute("categories", categoryService.findAllActive());
            return "admin/product-form";
        }
        try {
            productService.update(id, product, imageFile, specRequest);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("spec", specRequest);
            model.addAttribute("categories", categoryService.findAllActive());
            return "admin/product-form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật sản phẩm thành công.");
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/toggle")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.toggleStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái sản phẩm.");
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/products";
    }
}