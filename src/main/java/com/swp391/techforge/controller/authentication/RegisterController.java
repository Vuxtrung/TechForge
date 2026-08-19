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

@Controller
public class RegisterController {

    private final RegisterService registerService;

    public RegisterController(RegisterService registerService) {
        this.registerService = registerService;
    }

    /**
     * Hiển thị trang đăng ký tài khoản.
     * Cung cấp một đối tượng RegisterRequest rỗng cho Spring Form binding.
     * 
     * @param model Đối tượng chứa dữ liệu đẩy ra view
     * @return Tên template trang đăng ký (register.html)
     */
    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    /**
     * Xử lý yêu cầu đăng ký tài khoản từ người dùng.
     * Kiểm tra tính hợp lệ của form (rỗng, email trùng, password ngắn).
     * 
     * @param request Dữ liệu từ form đăng ký
     * @param bindingResult Kết quả validation của Spring
     * @param model Đối tượng chứa dữ liệu đẩy ra view
     * @return Chuyển hướng tới trang đăng nhập nếu thành công, hoặc trả lại trang đăng ký nếu có lỗi
     */
    @PostMapping("/register")
    public String processRegister(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult,
            Model model) {

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

        return "redirect:/login";
    }
}