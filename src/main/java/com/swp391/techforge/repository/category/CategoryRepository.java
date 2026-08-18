package com.swp391.techforge.repository.category;

import com.swp391.techforge.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Check trùng ở validatename
    boolean existsByNameIgnoreCase(String name);
    // Check trùng khi sửa
    boolean existsByNameIgnoreCaseAndCategoryIdNot(String name, Long categoryId);

    List<Category> findAllByActiveTrueOrderByNameAsc();

    // Lấy danh mục gốc (cha) đang active, dùng cho mega-menu ở header
    List<Category> findAllByParentIsNullAndActiveTrueOrderByNameAsc();

    // Lấy toàn bộ danh mục con trực tiếp của 1 danh mục (dùng cho cascade ẩn/hiện) đi qua field Parent và lấy categoryId của nó
    List<Category> findAllByParent_CategoryId(Long parentId); 

    // Lấy toàn bộ danh mục con trực tiếp của 1 danh mục đang active cho màn admin có thể lọc theo tên, type, active - mỗi điều kiện đều không bắt buộc
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

    // Lấy id của chính danh mục này VÀ toàn bộ danh mục con cháu (đệ quy),
    // dùng để lọc sản phẩm khi bấm vào 1 danh mục cha ở trang chủ / trang danh sách
    @Query(value = """
            WITH RECURSIVE category_tree AS (
                SELECT category_id FROM categories WHERE category_id = :categoryId
                UNION ALL
                SELECT c.category_id FROM categories c
                INNER JOIN category_tree ct ON c.parent_id = ct.category_id
            )
            SELECT category_id FROM category_tree
            """, nativeQuery = true)
    List<Long> findSelfAndDescendantIds(@Param("categoryId") Long categoryId);

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

          // Lấy ID của N danh mục gốc có nhiều sản phẩm nhất (tính cả sản phẩm của danh mục con, đệ quy)
    @Query(value = """
            WITH RECURSIVE category_tree AS (
                SELECT category_id, category_id AS root_id
                FROM categories
                WHERE parent_id IS NULL AND is_active = true
                UNION ALL
                SELECT c.category_id, ct.root_id
                FROM categories c
                INNER JOIN category_tree ct ON c.parent_id = ct.category_id
            )
            SELECT ct.root_id
            FROM category_tree ct
            INNER JOIN products p ON p.category_id = ct.category_id
            GROUP BY ct.root_id
            ORDER BY COUNT(p.product_id) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findTopRootCategoryIdsByProductCount(@Param("limit") int limit);
}