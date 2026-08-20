package com.swp391.techforge.controller.customer;

import com.swp391.techforge.entity.Blog;
import com.swp391.techforge.service.blog.BlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Controller phục vụ khách hàng đọc tin tức, bài viết công nghệ và thủ thuật máy tính
 * Tuyến đường: /blogs
 */
@Controller
@RequestMapping("/blogs")
public class BlogPublicController {

    @Autowired
    private BlogService blogService;

    /**
     * Trang chủ Tin Tức & Blog công nghệ: Hiển thị danh sách bài viết đã xuất bản, bộ lọc theo danh mục và tìm kiếm
     */
    @GetMapping
    public String listPublicBlogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            Model model) {

        Page<Blog> blogPage = blogService.getPublicBlogs(keyword, category, page, size);
        List<String> categories = blogService.getAllCategories();
        List<Blog> trendingBlogs = blogService.getTrendingBlogs();
        List<Blog> recentBlogs = blogService.getRecentBlogs();

        model.addAttribute("blogPage", blogPage);
        model.addAttribute("categories", categories);
        model.addAttribute("currentCategory", category);
        model.addAttribute("keyword", keyword);
        model.addAttribute("trendingBlogs", trendingBlogs);
        model.addAttribute("recentBlogs", recentBlogs);
        model.addAttribute("pageTitle", "Tin Tức & Blog Công Nghệ");

        return "customer/blog-list";
    }

    /**
     * Trang đọc chi tiết bài viết theo ID: Tự động tăng lượt xem và hiển thị bài viết liên quan
     */
    @GetMapping("/{id}")
    public String viewBlogDetailById(@PathVariable Long id, Model model) {
        try {
            Blog blog = blogService.getBlogById(id);
            
            // Tăng lượt xem bài viết
            blogService.incrementViews(id);

            List<Blog> relatedBlogs = blogService.getRelatedBlogs(blog.getId(), blog.getCategory());
            List<Blog> trendingBlogs = blogService.getTrendingBlogs();

            model.addAttribute("blog", blog);
            model.addAttribute("relatedBlogs", relatedBlogs);
            model.addAttribute("trendingBlogs", trendingBlogs);
            model.addAttribute("pageTitle", blog.getTitle());

            return "customer/blog-detail";

        } catch (Exception e) {
            return "redirect:/blogs";
        }
    }

    /**
     * Trang đọc chi tiết bài viết theo URL tĩnh chuẩn SEO (Slug)
     */
    @GetMapping("/detail/{slug}")
    public String viewBlogDetailBySlug(@PathVariable String slug, Model model) {
        try {
            Blog blog = blogService.getBlogBySlug(slug);
            blogService.incrementViews(blog.getId());

            List<Blog> relatedBlogs = blogService.getRelatedBlogs(blog.getId(), blog.getCategory());
            List<Blog> trendingBlogs = blogService.getTrendingBlogs();

            model.addAttribute("blog", blog);
            model.addAttribute("relatedBlogs", relatedBlogs);
            model.addAttribute("trendingBlogs", trendingBlogs);
            model.addAttribute("pageTitle", blog.getTitle());

            return "customer/blog-detail";

        } catch (Exception e) {
            return "redirect:/blogs";
        }
    }
}
