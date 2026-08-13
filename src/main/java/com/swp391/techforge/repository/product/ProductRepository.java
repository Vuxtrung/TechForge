package com.swp391.techforge.repository.product;

import com.swp391.techforge.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndProductIdNot(String name, Long productId);

    @Query("""
            SELECT p FROM Product p
            WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
              AND (:status IS NULL OR :status = '' OR CAST(p.status AS string) = :status)
            """)
    Page<Product> search(@Param("keyword") String keyword,
                          @Param("categoryId") Long categoryId,
                          @Param("status") String status,
                          Pageable pageable);
}