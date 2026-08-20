package com.swp391.techforge.controller.admin;

import com.swp391.techforge.entity.Review;
import com.swp391.techforge.repository.product.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/reviews")
public class AdminReviewController {

    private final ReviewRepository reviewRepository;

    public AdminReviewController(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @GetMapping
    public String listReviews(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviewPage = reviewRepository.findAllByOrderByCreatedAtDesc(pageable);
        model.addAttribute("reviewPage", reviewPage);
        return "admin/review-list";
    }

    @PostMapping("/{id}/delete")
    public String deleteReview(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reviewRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa đánh giá thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa đánh giá.");
        }
        return "redirect:/admin/reviews";
    }
}
