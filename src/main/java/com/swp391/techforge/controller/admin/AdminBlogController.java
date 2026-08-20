package com.swp391.techforge.controller.admin;

import com.swp391.techforge.dto.blog.BlogRequest;
import com.swp391.techforge.entity.Blog;
import com.swp391.techforge.entity.BlogStatus;
import com.swp391.techforge.entity.User;
import com.swp391.techforge.repository.authentication.UserRepository;
import com.swp391.techforge.service.blog.BlogService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

/**
 * Controller quản trị Blog dành cho Admin và Nhân viên (Staff)
 * Tuyến đường gốc: /admin/blogs
 */
@Controller
@RequestMapping("/admin/blogs")
public class AdminBlogController {

    @Autowired
    private BlogService blogService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Màn hình danh sách bài viết: Tìm kiếm, lọc theo danh mục/trạng thái, sắp xếp, phân trang và thao tác nhanh
     */
    @GetMapping
    public String listBlogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BlogStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model) {

        Page<Blog> blogPage = blogService.getAdminBlogs(keyword, category, status, page, size, sortBy, sortDir);
        List<String> categories = blogService.getAllCategories();

        model.addAttribute("blogPage", blogPage);
        model.addAttribute("categories", categories);
        model.addAttribute("statuses", BlogStatus.values());
        
        // Giữ lại các giá trị lọc trên thanh công cụ
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentCategory", category);
        model.addAttribute("currentStatus", status);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        return "admin/blog-list";
    }

    /**
     * Màn hình tạo mới bài viết (Dùng chung Form với màn hình xem chi tiết / chỉnh sửa)
     */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("blogRequest")) {
            BlogRequest request = new BlogRequest();
            request.setStatus(BlogStatus.DRAFT);
            model.addAttribute("blogRequest", request);
        }
        
        model.addAttribute("categories", blogService.getAllCategories());
        model.addAttribute("statuses", BlogStatus.values());
        model.addAttribute("formTitle", "Thêm Mới Bài Viết");
        model.addAttribute("isEditMode", false);
        
        return "admin/blog-form";
    }

    /**
     * Tiếp nhận và xử lý thêm mới bài viết
     */
    @PostMapping("/new")
    public String createBlog(
            @Valid @ModelAttribute("blogRequest") BlogRequest request,
            BindingResult bindingResult,
            Principal principal,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", blogService.getAllCategories());
            model.addAttribute("statuses", BlogStatus.values());
            model.addAttribute("formTitle", "Thêm Mới Bài Viết");
            model.addAttribute("isEditMode", false);
            return "admin/blog-form";
        }

        try {
            User author = null;
            if (principal != null) {
                author = userRepository.findByEmail(principal.getName()).orElse(null);
            }

            Blog created = blogService.createBlog(request, author);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm bài viết '" + created.getTitle() + "' thành công!");
            return "redirect:/admin/blogs";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Lỗi khi tạo bài viết: " + e.getMessage());
            model.addAttribute("categories", blogService.getAllCategories());
            model.addAttribute("statuses", BlogStatus.values());
            model.addAttribute("formTitle", "Thêm Mới Bài Viết");
            model.addAttribute("isEditMode", false);
            return "admin/blog-form";
        }
    }

    /**
     * Màn hình xem chi tiết và chỉnh sửa bài viết (Shared Form)
     */
    @GetMapping("/{id}")
    public String showEditAndDetailForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Blog blog = blogService.getBlogById(id);
            
            if (!model.containsAttribute("blogRequest")) {
                BlogRequest request = new BlogRequest();
                request.setId(blog.getId());
                request.setTitle(blog.getTitle());
                request.setSlug(blog.getSlug());
                request.setCategory(blog.getCategory());
                request.setSummary(blog.getSummary());
                request.setContent(blog.getContent());
                request.setThumbnailUrl(blog.getThumbnailUrl());
                request.setStatus(blog.getStatus());
                model.addAttribute("blogRequest", request);
            }

            model.addAttribute("blog", blog);
            model.addAttribute("categories", blogService.getAllCategories());
            model.addAttribute("statuses", BlogStatus.values());
            model.addAttribute("formTitle", "Chi Tiết & Chỉnh Sửa Bài Viết #" + blog.getId());
            model.addAttribute("isEditMode", true);

            return "admin/blog-form";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/blogs";
        }
    }

    /**
     * Xử lý cập nhật thông tin bài viết
     */
    @PostMapping("/{id}/edit")
    public String updateBlog(
            @PathVariable Long id,
            @Valid @ModelAttribute("blogRequest") BlogRequest request,
            BindingResult bindingResult,
            Principal principal,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            Blog blog = blogService.getBlogById(id);
            model.addAttribute("blog", blog);
            model.addAttribute("categories", blogService.getAllCategories());
            model.addAttribute("statuses", BlogStatus.values());
            model.addAttribute("formTitle", "Chi Tiết & Chỉnh Sửa Bài Viết #" + id);
            model.addAttribute("isEditMode", true);
            return "admin/blog-form";
        }

        try {
            User updater = null;
            if (principal != null) {
                updater = userRepository.findByEmail(principal.getName()).orElse(null);
            }

            Blog updated = blogService.updateBlog(id, request, updater);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật bài viết '" + updated.getTitle() + "' thành công!");
            return "redirect:/admin/blogs";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Lỗi khi cập nhật: " + e.getMessage());
            Blog blog = blogService.getBlogById(id);
            model.addAttribute("blog", blog);
            model.addAttribute("categories", blogService.getAllCategories());
            model.addAttribute("statuses", BlogStatus.values());
            model.addAttribute("formTitle", "Chi Tiết & Chỉnh Sửa Bài Viết #" + id);
            model.addAttribute("isEditMode", true);
            return "admin/blog-form";
        }
    }

    /**
     * Đổi trạng thái duyệt / xuất bản / từ chối nhanh từ bảng danh sách
     */
    @PostMapping("/{id}/status")
    public String changeStatus(
            @PathVariable Long id,
            @RequestParam("status") BlogStatus status,
            RedirectAttributes redirectAttributes) {
        try {
            Blog updated = blogService.changeBlogStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Đã chuyển trạng thái bài viết sang: " + updated.getStatus().getDisplayName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể đổi trạng thái: " + e.getMessage());
        }
        return "redirect:/admin/blogs";
    }

    /**
     * Xóa bài viết khỏi cơ sở dữ liệu
     */
    @PostMapping("/{id}/delete")
    public String deleteBlog(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            blogService.deleteBlog(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa bài viết thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Xóa bài viết thất bại: " + e.getMessage());
        }
        return "redirect:/admin/blogs";
    }
}
