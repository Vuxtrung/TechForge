package com.swp391.techforge.controller.authentication;

import com.swp391.techforge.dto.authentication.RegisterRequest;
import com.swp391.techforge.dto.authentication.VerifyOtpRequest;
import com.swp391.techforge.entity.OtpPurpose;
import com.swp391.techforge.service.authentication.OtpService;
import com.swp391.techforge.service.authentication.RegisterService;
import jakarta.servlet.http.HttpSession;
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

    private static final String SESSION_REGISTER_REQUEST = "PENDING_REGISTER_REQUEST";

    private final RegisterService registerService;
    private final OtpService otpService;

    public RegisterController(RegisterService registerService, OtpService otpService) {
        this.registerService = registerService;
        this.otpService = otpService;
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
            HttpSession session,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            registerService.validate(request);
        } catch (IllegalArgumentException e) {
            model.addAttribute("registerError", e.getMessage());
            return "register";
        }

        boolean sent = otpService.generateAndSend(request.getEmail(), OtpPurpose.REGISTER);
        if (!sent) {
            model.addAttribute("registerError", "Bạn vừa yêu cầu gửi OTP, vui lòng đợi ít phút rồi thử lại.");
            return "register";
        }
        session.setAttribute(SESSION_REGISTER_REQUEST, request);
        return "redirect:/register/verify";
    }

    @GetMapping("/register/verify")
    public String showVerifyOtpForm(HttpSession session, Model model) {
        RegisterRequest pending = (RegisterRequest) session.getAttribute(SESSION_REGISTER_REQUEST);
        if (pending == null) {
            return "redirect:/register";
        }

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail(pending.getEmail());
        model.addAttribute("verifyOtpRequest", request);
        model.addAttribute("maskedEmail", maskEmail(pending.getEmail()));
        return "register-verify-otp";
    }

    @PostMapping("/register/verify")
    public String submitOtp(@Valid VerifyOtpRequest request, BindingResult bindingResult,
            HttpSession session, Model model, RedirectAttributes redirectAttributes) {

        RegisterRequest pending = (RegisterRequest) session.getAttribute(SESSION_REGISTER_REQUEST);
        if (pending == null) {
            return "redirect:/register";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("maskedEmail", maskEmail(pending.getEmail()));
            return "register-verify-otp";
        }

        OtpService.VerifyResult result = otpService.verify(pending.getEmail(), OtpPurpose.REGISTER,
                request.getOtpCode());

        switch (result) {
            case SUCCESS -> {
                try {
                    registerService.register(pending);
                } catch (IllegalArgumentException e) {
                    session.removeAttribute(SESSION_REGISTER_REQUEST);
                    model.addAttribute("registerError", e.getMessage());
                    model.addAttribute("registerRequest", pending);
                    return "register";
                } catch (IllegalStateException e) {
                    session.removeAttribute(SESSION_REGISTER_REQUEST);
                    model.addAttribute("registerError", e.getMessage());
                    model.addAttribute("registerRequest", pending);
                    return "register";
                }

                session.removeAttribute(SESSION_REGISTER_REQUEST);
                redirectAttributes.addFlashAttribute("autofilledEmail", pending.getEmail());
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đăng ký thành công! Vui lòng đăng nhập bằng email của bạn.");
                return "redirect:/login";
            }
            case WRONG_CODE -> model.addAttribute("errorMessage", "Mã OTP không đúng, vui lòng thử lại.");
            case EXPIRED -> model.addAttribute("errorMessage", "Mã OTP đã hết hạn, vui lòng gửi lại.");
            case MAX_ATTEMPTS_REACHED ->
                model.addAttribute("errorMessage", "Bạn đã nhập sai quá số lần cho phép, vui lòng gửi lại OTP.");
            default -> model.addAttribute("errorMessage", "Có lỗi xảy ra, vui lòng thử lại.");
        }

        model.addAttribute("maskedEmail", maskEmail(pending.getEmail()));
        return "register-verify-otp";
    }

    @PostMapping("/register/resend")
    public String resendOtp(HttpSession session, RedirectAttributes redirectAttributes) {
        RegisterRequest pending = (RegisterRequest) session.getAttribute(SESSION_REGISTER_REQUEST);
        if (pending == null) {
            return "redirect:/register";
        }

        boolean sent = otpService.generateAndSend(pending.getEmail(), OtpPurpose.REGISTER);
        redirectAttributes.addFlashAttribute(
                sent ? "successMessage" : "errorMessage",
                sent ? "Đã gửi lại mã OTP." : "Vui lòng đợi ít phút trước khi gửi lại OTP.");
        return "redirect:/register/verify";
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 2)
            return email;
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}