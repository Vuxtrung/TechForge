package com.swp391.techforge.dto.product;

import com.swp391.techforge.entity.Category;

import java.util.List;

/**
 * Dữ liệu đổ vào sidebar filter của trang /products.
 * Khác với ProductFilterRequest (đầu vào từ URL), object này là đầu ra
 * do Service tổng hợp từ DB để Controller đưa vào Model cho View render.
 */
public class ProductFilterOptions {

    private List<Category> categories;

    private List<String> brands;

    public ProductFilterOptions(List<Category> categories, List<String> brands) {
        this.categories = categories;
        this.brands = brands;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public List<String> getBrands() {
        return brands;
    }

    public void setBrands(List<String> brands) {
        this.brands = brands;
    }
}