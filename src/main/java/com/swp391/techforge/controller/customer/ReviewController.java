package com.swp391.techforge.controller.customer;

import com.swp391.techforge.entity.User;
import com.swp391.techforge.service.product.ReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/customer/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/add")
    public String addReview(@RequestParam("orderItemId") Long orderItemId,
                            @RequestParam("orderId") Long orderId,
                            @RequestParam("rating") Integer rating,
                            @RequestParam("comment") String comment,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        try {
            reviewService.addReview(loggedInUser, orderItemId, rating, comment);
            redirectAttributes.addFlashAttribute("successMessage", "Cảm ơn bạn đã đánh giá sản phẩm!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/orders/" + orderId;
    }
}
