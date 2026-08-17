package com.swp391.techforge.controller.home;

import com.swp391.techforge.entity.Product;
import com.swp391.techforge.service.category.CategoryService;
import com.swp391.techforge.service.product.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public HomeController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping("/")
    public String home(Model model) {
        // 1. Fetch categories for Homepage Menu
        model.addAttribute("categories", categoryService.findRootCategoriesForNav());

        // 2. Fetch top 8 active products for Flash Sale
        Page<Product> productPage = productService.search(null, null, "ACTIVE", 0, 8, Sort.by(Sort.Direction.DESC, "productId"));
        model.addAttribute("latestProducts", productPage.getContent());

        // 3. Fetch 4 active products for PC Gaming Showcase
        Page<Product> gamingPage = productService.search(null, null, "ACTIVE", 0, 4, Sort.by(Sort.Direction.ASC, "productId"));
        model.addAttribute("gamingProducts", gamingPage.getContent());

        return "home";
    }

    @GetMapping("/home")
    public String redirectHome() {
        return "redirect:/";
    }

    @GetMapping("/buildpc")
    public String buildpc(Model model) {
        model.addAttribute("pageTitle", "Tự Build PC");
        return "buildpc";
    }
}