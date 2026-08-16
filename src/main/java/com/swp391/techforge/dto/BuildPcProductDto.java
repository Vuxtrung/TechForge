package com.swp391.techforge.dto;

import com.swp391.techforge.entity.Product;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BuildPcProductDto {
    private Long productId;
    private String name;
    private String brand;
    private BigDecimal basePrice;
    private String primaryImageUrl;
    private Integer stockQuantity;
    private String keySpec; // For example: CPU socket, RAM type

    public BuildPcProductDto(Product product) {
        this.productId = product.getProductId();
        this.name = product.getName();
        this.brand = product.getBrand();
        this.basePrice = product.getBasePrice();
        this.primaryImageUrl = product.getPrimaryImageUrl();
        this.stockQuantity = product.getStockQuantity();
    }
}
