package com.swp391.techforge.service.blog;

import com.swp391.techforge.dto.blog.BlogRequest;
import com.swp391.techforge.entity.Blog;
import com.swp391.techforge.entity.BlogStatus;
import com.swp391.techforge.entity.User;
import com.swp391.techforge.repository.blog.BlogRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Service xử lý toàn bộ nghiệp vụ quản lý Blog và Tin tức công nghệ
 */
@Service
public class BlogService {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    @Autowired
    private BlogRepository blogRepository;

    /**
     * Lấy danh sách bài viết cho trang quản trị với bộ lọc động, tìm kiếm, phân trang và sắp xếp
     */
    @Transactional(readOnly = true)
    public Page<Blog> getAdminBlogs(String keyword, String category, BlogStatus status,
                                    int page, int size, String sortBy, String sortDir) {
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir != null && sortDir.equalsIgnoreCase("asc") ? "ASC" : "DESC"),
                (sortBy != null && !sortBy.trim().isEmpty()) ? sortBy : "createdAt");
        
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);

        Specification<Blog> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Bộ lọc từ khóa tìm kiếm (theo Tiêu đề, Tóm tắt hoặc Nội dung)
            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchPattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate titleLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), searchPattern);
                Predicate summaryLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("summary")), searchPattern);
                Predicate contentLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("content")), searchPattern);
                predicates.add(criteriaBuilder.or(titleLike, summaryLike, contentLike));
            }

            // Bộ lọc theo danh mục
            if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category)) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category.trim()));
            }

            // Bộ lọc theo trạng thái duyệt / xuất bản
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return blogRepository.findAll(spec, pageable);
    }

    /**
     * Lấy danh sách bài viết công khai cho khách hàng (chỉ lấy trạng thái PUBLISHED)
     */
    @Transactional(readOnly = true)
    public Page<Blog> getPublicBlogs(String keyword, String category, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Blog> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("status"), BlogStatus.PUBLISHED));

            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchPattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate titleLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), searchPattern);
                Predicate summaryLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("summary")), searchPattern);
                predicates.add(criteriaBuilder.or(titleLike, summaryLike));
            }

            if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category)) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category.trim()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return blogRepository.findAll(spec, pageable);
    }

    /**
     * Tìm bài viết theo ID
     */
    @Transactional(readOnly = true)
    public Blog getBlogById(Long id) {
        return blogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết với ID: " + id));
    }

    /**
     * Tìm bài viết theo đường dẫn tĩnh (Slug)
     */
    @Transactional(readOnly = true)
    public Blog getBlogBySlug(String slug) {
        return blogRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết với slug: " + slug));
    }

    /**
     * Thêm mới một bài viết blog vào hệ thống
     */
    @Transactional
    public Blog createBlog(BlogRequest request, User author) {
        Blog blog = new Blog();
        blog.setTitle(request.getTitle().trim());
        
        // Tạo slug chuẩn SEO từ tiêu đề bài viết
        String generatedSlug = (request.getSlug() != null && !request.getSlug().trim().isEmpty())
                ? makeSlug(request.getSlug().trim())
                : makeSlug(request.getTitle().trim());
        
        blog.setSlug(ensureUniqueSlug(generatedSlug, null));
        blog.setCategory(request.getCategory().trim());
        blog.setSummary(request.getSummary().trim());
        blog.setContent(request.getContent());
        blog.setThumbnailUrl((request.getThumbnailUrl() != null && !request.getThumbnailUrl().trim().isEmpty())
                ? request.getThumbnailUrl().trim() : "/images/default-blog.jpg");
        
        blog.setStatus(request.getStatus() != null ? request.getStatus() : BlogStatus.DRAFT);
        blog.setAuthor(author);
        blog.setViewsCount(0);

        if (blog.getStatus() == BlogStatus.PUBLISHED) {
            blog.setPublishedAt(LocalDateTime.now());
        }

        return blogRepository.save(blog);
    }

    /**
     * Cập nhật thông tin bài viết hiện có
     */
    @Transactional
    public Blog updateBlog(Long id, BlogRequest request, User updater) {
        Blog blog = getBlogById(id);

        blog.setTitle(request.getTitle().trim());
        
        String newSlug = (request.getSlug() != null && !request.getSlug().trim().isEmpty())
                ? makeSlug(request.getSlug().trim())
                : makeSlug(request.getTitle().trim());
        blog.setSlug(ensureUniqueSlug(newSlug, id));

        blog.setCategory(request.getCategory().trim());
        blog.setSummary(request.getSummary().trim());
        blog.setContent(request.getContent());
        
        if (request.getThumbnailUrl() != null && !request.getThumbnailUrl().trim().isEmpty()) {
            blog.setThumbnailUrl(request.getThumbnailUrl().trim());
        }

        BlogStatus oldStatus = blog.getStatus();
        blog.setStatus(request.getStatus() != null ? request.getStatus() : BlogStatus.DRAFT);

        if (oldStatus != BlogStatus.PUBLISHED && blog.getStatus() == BlogStatus.PUBLISHED) {
            blog.setPublishedAt(LocalDateTime.now());
        }

        return blogRepository.save(blog);
    }

    /**
     * Đổi trạng thái duyệt / xuất bản / từ chối nhanh bài viết
     */
    @Transactional
    public Blog changeBlogStatus(Long id, BlogStatus newStatus) {
        Blog blog = getBlogById(id);
        if (blog.getStatus() != BlogStatus.PUBLISHED && newStatus == BlogStatus.PUBLISHED) {
            blog.setPublishedAt(LocalDateTime.now());
        }
        blog.setStatus(newStatus);
        return blogRepository.save(blog);
    }

    /**
     * Xóa bài viết khỏi hệ thống
     */
    @Transactional
    public void deleteBlog(Long id) {
        if (!blogRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy bài viết để xóa với ID: " + id);
        }
        blogRepository.deleteById(id);
    }

    /**
     * Tăng số lượt xem bài viết khi khách hàng truy cập
     */
    @Transactional
    public void incrementViews(Long id) {
        blogRepository.incrementViewsCount(id);
    }

    /**
     * Lấy danh sách bài viết thịnh hành
     */
    @Transactional(readOnly = true)
    public List<Blog> getTrendingBlogs() {
        return blogRepository.findTop5ByStatusOrderByViewsCountDesc(BlogStatus.PUBLISHED);
    }

    /**
     * Lấy danh sách bài viết mới nhất
     */
    @Transactional(readOnly = true)
    public List<Blog> getRecentBlogs() {
        return blogRepository.findTop4ByStatusOrderByCreatedAtDesc(BlogStatus.PUBLISHED);
    }

    /**
     * Lấy danh sách bài viết liên quan
     */
    @Transactional(readOnly = true)
    public List<Blog> getRelatedBlogs(Long currentBlogId, String category) {
        if (category != null && !category.trim().isEmpty()) {
            return blogRepository.findTop4ByStatusAndCategoryAndIdNotOrderByCreatedAtDesc(
                    BlogStatus.PUBLISHED, category.trim(), currentBlogId);
        }
        return blogRepository.findTop4ByStatusOrderByCreatedAtDesc(BlogStatus.PUBLISHED);
    }

    /**
     * Lấy danh sách toàn bộ danh mục bài viết đang có
     */
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return blogRepository.findDistinctCategories();
    }

    /**
     * Chuyển đổi chuỗi tiếng Việt có dấu thành URL slug không dấu chuẩn SEO
     */
    public static String makeSlug(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "bai-viet-" + System.currentTimeMillis();
        }
        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        slug = slug.toLowerCase(Locale.ENGLISH);
        slug = slug.replaceAll("-+", "-").replaceAll("^-|-$", "");
        return slug.isEmpty() ? "bai-viet-" + System.currentTimeMillis() : slug;
    }

    /**
     * Đảm bảo Slug không bị trùng lặp trong cơ sở dữ liệu
     */
    private String ensureUniqueSlug(String baseSlug, Long excludeId) {
        String slug = baseSlug;
        int count = 1;
        while (true) {
            boolean exists = (excludeId == null)
                    ? blogRepository.existsBySlug(slug)
                    : blogRepository.existsBySlugAndIdNot(slug, excludeId);
            if (!exists) {
                return slug;
            }
            slug = baseSlug + "-" + count++;
        }
    }
}
