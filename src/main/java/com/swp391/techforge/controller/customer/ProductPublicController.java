package com.swp391.techforge.controller.customer;

import com.swp391.techforge.dto.product.ProductFilterRequest;
import com.swp391.techforge.entity.Product;
import com.swp391.techforge.service.product.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * F_07 - Product Listing & Search.
 * Trang khách xem danh sách sản phẩm, có search + filter (danh mục, giá, thương hiệu).
 */
@Controller
public class ProductPublicController {

    private final ProductService productService;

    public ProductPublicController(ProductService productService) {
        this.productService = productService;
    }

    // Spring tự bind toàn bộ query param (keyword, categoryId, brand, minPrice,
    // maxPrice, sort, page) vào ProductFilterRequest nhờ @ModelAttribute -
    // không cần khai báo từng @RequestParam.
    @GetMapping("/products")
    public String list(@ModelAttribute ProductFilterRequest filter, Model model) {
        Page<Product> productPage = productService.searchPublic(filter);

        model.addAttribute("productPage", productPage);
        model.addAttribute("filter", filter);
        model.addAttribute("filterOptions", productService.getFilterOptions());
        return "customer/products";
    }
}