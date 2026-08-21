package com.swp391.techforge.repository.product;

import com.swp391.techforge.entity.ProductSpecification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductSpecificationRepository extends JpaRepository<ProductSpecification, Long> {
    void deleteAllByProduct_ProductId(Long productId);

    List<ProductSpecification> findAllByProduct_ProductId(Long productId);
}