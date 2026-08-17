package com.swp391.techforge.controller.home;

import com.swp391.techforge.entity.Product;
import com.swp391.techforge.service.product.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ProductService productService;

    public HomeController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/")
    public String home(Model model) {
        // Fetch top 8 active products for Flash Sale
        Page<Product> productPage = productService.search(null, null, "ACTIVE", 0, 8, Sort.by(Sort.Direction.DESC, "productId"));
        model.addAttribute("latestProducts", productPage.getContent());

        // Fetch 4 active products for PC Gaming Showcase
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