package com.swp391.techforge.controller.authentication;

import com.swp391.techforge.dto.account.AccountInfoRequest;
import com.swp391.techforge.dto.account.ChangePasswordRequest;
import com.swp391.techforge.entity.Order;
import com.swp391.techforge.entity.User;
import com.swp391.techforge.repository.authentication.UserRepository;
import com.swp391.techforge.service.order.OrderService;
import com.swp391.techforge.service.product.CloudinaryService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/account")
public class AccountController {

    private static final Set<String> ALLOWED_VIEWS = Set.of("info", "orders", "password");

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final OrderService orderService;
    private final PasswordEncoder passwordEncoder;

    public AccountController(
            UserRepository userRepository,
            CloudinaryService cloudinaryService,
            OrderService orderService,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
        this.orderService = orderService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String account(
            @RequestParam(value = "view", defaultValue = "none") String view,
            Authentication authentication,
            Model model) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy tài khoản với email: " + email));

        model.addAttribute("user", user);
        model.addAttribute("view", ALLOWED_VIEWS.contains(view) ? view : null);

        if ("orders".equals(view)) {
            List<Order> orders = orderService.getCustomerOrders(user, null, null, null, null);
            model.addAttribute("orders", orders);
        }

        if (!model.containsAttribute("accountInfoRequest")) {
            AccountInfoRequest infoReq = new AccountInfoRequest();
            infoReq.setFullName(user.getFullName());
            infoReq.setPhone(user.getPhone());
            infoReq.setAddress(user.getAddress());
            model.addAttribute("accountInfoRequest", infoReq);
        }

        if (!model.containsAttribute("changePasswordRequest")) {
            model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        }

        return "account";
    }

    @PostMapping("/info")
    public String updateInfo(
            @Valid @ModelAttribute("accountInfoRequest") AccountInfoRequest request,
            BindingResult bindingResult,
            Authentication authentication,
            Model model) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản"));

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            model.addAttribute("view", "info");
            return "account";
        }

        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());

        MultipartFile avatarFile = request.getAvatarFile();
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                user.setAvatarUrl(cloudinaryService.uploadImage(avatarFile, "techforge/avatars"));
            } catch (IOException e) {
                bindingResult.reject("avatarUpload", "Tải ảnh đại diện thất bại, vui lòng thử lại.");
                model.addAttribute("user", user);
                model.addAttribute("view", "info");
                return "account";
            }
        }

        userRepository.save(user);

        AccountInfoRequest refreshed = new AccountInfoRequest();
        refreshed.setFullName(user.getFullName());
        refreshed.setPhone(user.getPhone());
        refreshed.setAddress(user.getAddress());

        model.addAttribute("user", user);
        model.addAttribute("view", "info");
        model.addAttribute("accountInfoRequest", refreshed);
        model.addAttribute("successMessage", "Cập nhật thông tin tài khoản thành công.");

        return "account";
    }

    @PostMapping("/password")
    public String changePassword(
            @Valid @ModelAttribute("changePasswordRequest") ChangePasswordRequest request,
            BindingResult bindingResult,
            Authentication authentication,
            Model model) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản"));

        if (!bindingResult.hasErrors()
                && !request.getNewPassword().equals(request.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.confirmPassword", "Mật khẩu xác nhận không khớp");
        }

        if (!bindingResult.hasErrors()
                && !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            bindingResult.rejectValue("currentPassword", "error.currentPassword", "Mật khẩu hiện tại không đúng");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            model.addAttribute("view", "password");
            return "account";
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        model.addAttribute("user", user);
        model.addAttribute("view", "password");
        model.addAttribute("successMessage", "Đổi mật khẩu thành công.");
        model.addAttribute("changePasswordRequest", new ChangePasswordRequest());

        return "account";
    }
}