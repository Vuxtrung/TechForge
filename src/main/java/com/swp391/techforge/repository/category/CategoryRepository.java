package com.swp391.techforge.repository.category;

import com.swp391.techforge.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndCategoryIdNot(String name, Long categoryId);

    List<Category> findAllByActiveTrueOrderByNameAsc();

    // Lấy danh mục gốc (cha) đang active, dùng cho mega-menu ở header
    List<Category> findAllByParentIsNullAndActiveTrueOrderByNameAsc();

    // Lấy toàn bộ danh mục con trực tiếp của 1 danh mục (dùng cho cascade ẩn/hiện)
    List<Category> findAllByParent_CategoryId(Long parentId);

    // Lấy toàn bộ danh mục con trực tiếp của 1 danh mục đang active (dùng cho dropdown filter)
    @Query("""
            SELECT c FROM Category c
            WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:type IS NULL OR :type = '' OR CAST(c.type AS string) = :type)
              AND (:active IS NULL OR c.active = :active)
            """)
    Page<Category> search(@Param("keyword") String keyword,
                           @Param("type") String type,
                           @Param("active") Boolean active,
                           Pageable pageable);

    // Đếm sản phẩm trực tiếp của 1 danh mục ( để chặn xóa category còn sản phẩm)
    @Query(value = "SELECT COUNT(*) FROM products WHERE category_id = :categoryId", nativeQuery = true)
    long countProductsByCategoryId(@Param("categoryId") Long categoryId);

    // Đếm sản phẩm của danh mục VÀ tất cả danh mục con (đệ quy)
    @Query(value = """
            WITH RECURSIVE category_tree AS (
                SELECT category_id FROM categories WHERE category_id = :categoryId
                UNION ALL
                SELECT c.category_id FROM categories c
                INNER JOIN category_tree ct ON c.parent_id = ct.category_id
            )
            SELECT COUNT(*) FROM products
            WHERE category_id IN (SELECT category_id FROM category_tree)
            """, nativeQuery = true)
    long countProductsByCategoryIdRecursive(@Param("categoryId") Long categoryId);
}