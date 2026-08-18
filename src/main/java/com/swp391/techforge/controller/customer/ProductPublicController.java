package com.swp391.techforge.controller.customer;

import com.swp391.techforge.dto.product.ProductFilterRequest;
import com.swp391.techforge.entity.Product;
import com.swp391.techforge.service.product.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * F_07 - Product Listing & Search.
 * Trang khách xem danh sách sản phẩm, có search + filter (danh mục, giá, thương hiệu).
 * F_13 - Product Detail.
 * Trang khách xem chi tiết 1 sản phẩm.
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

    // F_13: xem chi tiết 1 sản phẩm + gợi ý vài sản phẩm cùng danh mục
    @GetMapping("/products/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Product product = productService.getById(id);
        if (product.getStatus() != Product.ProductStatus.ACTIVE) {
            // Không lộ thông tin sản phẩm đang bị ẩn cho khách qua URL trực tiếp
            throw new IllegalArgumentException("Không tìm thấy sản phẩm.");
        }

        List<Product> relatedProducts = List.of();
        if (product.getCategoryId() != null) {
            ProductFilterRequest relatedFilter = new ProductFilterRequest();
            relatedFilter.setCategoryId(product.getCategoryId());
            Page<Product> related = productService.searchPublic(relatedFilter);
            relatedProducts = related.getContent().stream()
                    .filter(p -> !p.getProductId().equals(id))
                    .limit(4)
                    .toList();
        }

        model.addAttribute("product", product);
        model.addAttribute("relatedProducts", relatedProducts);
        return "customer/product-detail";
    }
}