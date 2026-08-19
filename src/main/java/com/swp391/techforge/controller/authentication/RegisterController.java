package com.swp391.techforge.controller.authentication;

import com.swp391.techforge.dto.authentication.RegisterRequest;
import com.swp391.techforge.service.authentication.RegisterService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegisterController {

    private final RegisterService registerService;

    public RegisterController(RegisterService registerService) {
        this.registerService = registerService;
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Validation lỗi
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            registerService.register(request);
        } catch (IllegalArgumentException e) {
            model.addAttribute("registerError", e.getMessage());
            return "register";
        }

        redirectAttributes.addFlashAttribute("registeredEmail", request.getEmail());
        redirectAttributes.addFlashAttribute("successMessage",
                "Đăng ký thành công! Vui lòng đăng nhập bằng email của bạn.");
        return "redirect:/login";
    }
}