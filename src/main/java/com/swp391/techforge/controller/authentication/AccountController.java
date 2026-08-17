package com.swp391.techforge.controller.authentication;

import com.swp391.techforge.dto.account.AccountInfoRequest;
import com.swp391.techforge.entity.Order;
import com.swp391.techforge.entity.User;
import com.swp391.techforge.repository.authentication.UserRepository;
import com.swp391.techforge.service.order.OrderService;
import com.swp391.techforge.service.product.CloudinaryService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/account")
public class AccountController {

    private static final String ALLOWED_VIEWS_KEY = "orders";
    private static final Set<String> ALLOWED_VIEWS = Set.of("info", "orders", "password");

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final OrderService orderService;

    public AccountController(UserRepository userRepository, CloudinaryService cloudinaryService, OrderService orderService) {
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
        this.orderService = orderService;
    }

    @GetMapping
    public String account(
            @RequestParam(value = "view", defaultValue = "none") String view,
            Authentication authentication,
            Model model) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy tài khoản với email: " + email
                ));

        model.addAttribute("user", user);
        model.addAttribute("view", ALLOWED_VIEWS.contains(view) ? view : null);

        if ("orders".equals(view)) {
            List<Order> orders = orderService.getCustomerOrders(user, null, null, null, null);
            model.addAttribute("orders", orders);
        }

        return "account";
    }

    @PostMapping("/info")
    public String updateInfo(
            @Valid AccountInfoRequest request,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    bindingResult.getFieldErrors().get(0).getDefaultMessage()
            );
            return "redirect:/account?view=info";
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản"));

        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());

        MultipartFile avatarFile = request.getAvatarFile();
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                user.setAvatarUrl(cloudinaryService.uploadImage(avatarFile, "techforge/avatars"));
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage", "Tải ảnh đại diện thất bại, vui lòng thử lại."
                );
                return "redirect:/account?view=info";
            }
        }

        userRepository.save(user);

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin tài khoản thành công.");
        return "redirect:/account?view=info";
    }
}