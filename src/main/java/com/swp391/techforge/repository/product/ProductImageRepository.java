package com.swp391.techforge.repository.product;

import com.swp391.techforge.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProduct_ProductId(Long productId);

    void deleteByProduct_ProductId(Long productId);
}