package com.swp391.techforge.dto.product;

import java.math.BigDecimal;

/**
 * Gom toàn bộ query param của trang GET /products vào 1 object.
 * Spring tự bind param trên URL vào field cùng tên (không cần @RequestParam từng cái).
 * Ví dụ: /products?keyword=ryzen&categoryId=2&sort=priceAsc&page=1
 *   -> keyword="ryzen", categoryId=2, sort="priceAsc", page=1
 */
public class ProductFilterRequest {

    private String keyword;

    private Long categoryId;

    // "PC_PRODUCT" hoac "PC_COMPONENT" - loc theo loai danh muc tu 2 nut homepage
    private String type;

    private String brand;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    // Giá trị cho phép: "newest" (mặc định), "priceAsc", "priceDesc"
    private String sort = "newest";

    // Trang hiện tại, bắt đầu từ 0 (theo convention của Spring Data Pageable)
    private int page = 0;

    private static final int PAGE_SIZE = 12;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return PAGE_SIZE;
    }
}