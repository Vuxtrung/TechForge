package com.swp391.techforge.controller.customer;

import com.swp391.techforge.entity.User;
import com.swp391.techforge.service.product.ReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.swp391.techforge.repository.authentication.UserRepository;

@Controller
@RequestMapping("/customer/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    public ReviewController(ReviewService reviewService, UserRepository userRepository) {
        this.reviewService = reviewService;
        this.userRepository = userRepository;
    }

    @PostMapping("/add")
    public String addReview(@RequestParam("orderItemId") Long orderItemId,
                            @RequestParam("orderId") Long orderId,
                            @RequestParam("rating") Integer rating,
                            @RequestParam("comment") String comment,
                            java.security.Principal principal,
                            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return "redirect:/login";
        }

        User loggedInUser = userRepository.findByEmail(principal.getName()).orElse(null);
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
