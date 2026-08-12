package com.swp391.techforge.dto.cart;

/**
 * Class DTO lưu thông tin 1 món hàng trong giỏ hàng.
 * Người thực hiện: Cáp Duy Thái (F_09 - Add to Cart / View Cart)
 */
public class CartItemDTO {
    private Long productId;
    private String productName;
    private String imageUrl;
    private Double price;
    private Integer quantity;

    public CartItemDTO() {
    }

    public CartItemDTO(Long productId, String productName, String imageUrl, Double price, Integer quantity) {
        this.productId = productId;
        this.productName = productName;
        this.imageUrl = imageUrl;
        this.price = price;
        this.quantity = quantity;
    }

    // Hàm tính tổng tiền từng dòng món hàng (Giá * Số lượng)
    public Double getTotalPrice() {
        if (price == null || quantity == null) return 0.0;
        return price * quantity;
    }

    // các Getter và Setter cơ bản
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
