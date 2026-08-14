package com.swp391.techforge.controller.authentication;

import com.swp391.techforge.entity.User;
import com.swp391.techforge.repository.authentication.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/account")
public class AccountController {

    private final UserRepository userRepository;

    public AccountController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public String account(
            @RequestParam(value = "view", defaultValue = "none") String view,
            Authentication authentication,
            Model model) {

        // Lấy email của tài khoản đang đăng nhập
        String email = authentication.getName();

        // Tìm User trong database
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy tài khoản với email: " + email
                ));

        // Truyền User sang account.html
        model.addAttribute("user", user);

        // Truyền view hiện tại
        model.addAttribute("view", view);

        return "account";
    }
}