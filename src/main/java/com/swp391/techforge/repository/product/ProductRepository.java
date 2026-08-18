package com.swp391.techforge.repository.product;

import com.swp391.techforge.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndProductIdNot(String name, Long productId);

    // Dùng cho trang admin: tìm kiếm sản phẩm theo keyword, categoryId, status
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

    // Dùng cho trang khách xem /products: chỉ lấy sản phẩm ACTIVE, có thêm lọc giá & thương hiệu
    @Query("""
            SELECT p FROM Product p
            WHERE CAST(p.status AS string) = 'ACTIVE'
              AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
              AND (:type IS NULL OR :type = '' OR CAST(p.category.type AS string) = :type)
              AND (:brand IS NULL OR :brand = '' OR p.brand = :brand)
              AND (:minPrice IS NULL OR p.basePrice >= :minPrice)
              AND (:maxPrice IS NULL OR p.basePrice <= :maxPrice)
            """)
    Page<Product> searchPublic(@Param("keyword") String keyword,
                                @Param("categoryId") Long categoryId,
                                @Param("type") String type,
                                @Param("brand") String brand,
                                @Param("minPrice") BigDecimal minPrice,
                                @Param("maxPrice") BigDecimal maxPrice,
                                Pageable pageable);

    // Danh sách thương hiệu duy nhất, dùng cho checkbox filter bên sidebar
    @Query("""
            SELECT DISTINCT p.brand FROM Product p
            WHERE p.brand IS NOT NULL AND p.brand <> ''
            ORDER BY p.brand ASC
            """)
    List<String> findDistinctBrands();
}