package com.swp391.techforge.repository.blog;

import com.swp391.techforge.entity.Blog;
import com.swp391.techforge.entity.BlogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy xuất và thao tác dữ liệu bài viết Blog trong cơ sở dữ liệu
 */
@Repository
public interface BlogRepository extends JpaRepository<Blog, Long>, JpaSpecificationExecutor<Blog> {

    /**
     * Tìm bài viết theo đường dẫn tĩnh (Slug)
     */
    Optional<Blog> findBySlug(String slug);

    /**
     * Kiểm tra sự tồn tại của slug khi tạo mới
     */
    boolean existsBySlug(String slug);

    /**
     * Kiểm tra sự tồn tại của slug khi cập nhật bài viết khác
     */
    boolean existsBySlugAndIdNot(String slug, Long id);

    /**
     * Lấy danh sách các danh mục blog duy nhất hiện có trong hệ thống
     */
    @Query("SELECT DISTINCT b.category FROM Blog b WHERE b.category IS NOT NULL AND TRIM(b.category) != '' ORDER BY b.category ASC")
    List<String> findDistinctCategories();

    /**
     * Lấy danh sách bài viết thịnh hành nhiều lượt xem nhất
     */
    List<Blog> findTop5ByStatusOrderByViewsCountDesc(BlogStatus status);

    /**
     * Lấy danh sách bài viết mới nhất đã xuất bản
     */
    List<Blog> findTop4ByStatusOrderByCreatedAtDesc(BlogStatus status);

    /**
     * Lấy danh sách bài viết liên quan cùng danh mục
     */
    List<Blog> findTop4ByStatusAndCategoryAndIdNotOrderByCreatedAtDesc(BlogStatus status, String category, Long excludeId);

    /**
     * Tăng số lượt xem của bài viết lên 1
     */
    @Modifying
    @Query("UPDATE Blog b SET b.viewsCount = b.viewsCount + 1 WHERE b.id = :id")
    void incrementViewsCount(@Param("id") Long id);

    /**
     * Đếm số lượng bài viết theo trạng thái
     */
    long countByStatus(BlogStatus status);
}
