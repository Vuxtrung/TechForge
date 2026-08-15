package com.swp391.techforge.repository.product;

import com.swp391.techforge.entity.ProductSpecification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSpecificationRepository extends JpaRepository<ProductSpecification, Long> {
    void deleteAllByProduct_ProductId(Long productId);
}