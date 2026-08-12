package com.swp391.techforge.controller.authentication;

import com.swp391.techforge.dto.authentication.LoginRequest;
import com.swp391.techforge.service.authentication.LoginService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(
            @ModelAttribute LoginRequest request,
            Model model) {

        try {
            loginService.login(request);

            return "redirect:/";

        } catch (IllegalArgumentException e) {

            model.addAttribute("loginError", e.getMessage());

            return "login";
        }
    }
}