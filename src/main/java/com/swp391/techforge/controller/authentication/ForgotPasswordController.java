package com.swp391.techforge.controller.authentication;

import com.swp391.techforge.dto.authentication.ForgotPasswordRequest;
import com.swp391.techforge.dto.authentication.ResetPasswordRequest;
import com.swp391.techforge.dto.authentication.VerifyOtpRequest;
import com.swp391.techforge.entity.OtpPurpose;
import com.swp391.techforge.entity.User;
import com.swp391.techforge.repository.authentication.UserRepository;
import com.swp391.techforge.service.authentication.OtpService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ForgotPasswordController {

    private static final String SESSION_EMAIL = "FORGOT_PASSWORD_EMAIL";
    private static final String SESSION_OTP_VERIFIED = "FORGOT_PASSWORD_OTP_VERIFIED";

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;

    public ForgotPasswordController(UserRepository userRepository, OtpService otpService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
    }

    // ===== BƯỚC 1: NHẬP EMAIL =====
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest());
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String submitEmail(@Valid ForgotPasswordRequest request, BindingResult bindingResult,
                               HttpSession session, Model model) {

        if (bindingResult.hasErrors()) {
            return "forgot-password";
        }

        boolean userExists = userRepository.findByEmail(request.getEmail()).isPresent();

        // Không tiết lộ email có tồn tại hay không (tránh user enumeration) -
        // chỉ thực sự gửi khi email tồn tại, nhưng luôn chuyển bước như nhau.
        if (userExists) {
            boolean sent = otpService.generateAndSend(request.getEmail(), OtpPurpose.RESET_PASSWORD);
            if (!sent) {
                model.addAttribute("errorMessage", "Bạn vừa yêu cầu gửi OTP, vui lòng đợi ít phút rồi thử lại.");
                return "forgot-password";
            }
        }

        session.setAttribute(SESSION_EMAIL, request.getEmail());
        session.setAttribute(SESSION_OTP_VERIFIED, false);

        return "redirect:/forgot-password/verify";
    }

    // ===== BƯỚC 2: NHẬP OTP =====
    @GetMapping("/forgot-password/verify")
    public String showVerifyOtpForm(HttpSession session, Model model) {
        String email = (String) session.getAttribute(SESSION_EMAIL);
        if (email == null) {
            return "redirect:/forgot-password";
        }

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail(email);
        model.addAttribute("verifyOtpRequest", request);
        model.addAttribute("maskedEmail", maskEmail(email));
        return "verify-otp";
    }

    @PostMapping("/forgot-password/verify")
    public String submitOtp(@Valid VerifyOtpRequest request, BindingResult bindingResult,
                             HttpSession session, Model model) {

        String email = (String) session.getAttribute(SESSION_EMAIL);
        if (email == null) {
            return "redirect:/forgot-password";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("maskedEmail", maskEmail(email));
            return "verify-otp";
        }

        OtpService.VerifyResult result = otpService.verify(email, OtpPurpose.RESET_PASSWORD, request.getOtpCode());

        switch (result) {
            case SUCCESS -> {
                session.setAttribute(SESSION_OTP_VERIFIED, true);
                return "redirect:/reset-password";
            }
            case WRONG_CODE -> model.addAttribute("errorMessage", "Mã OTP không đúng, vui lòng thử lại.");
            case EXPIRED -> model.addAttribute("errorMessage", "Mã OTP đã hết hạn, vui lòng gửi lại.");
            case MAX_ATTEMPTS_REACHED -> model.addAttribute("errorMessage", "Bạn đã nhập sai quá số lần cho phép, vui lòng gửi lại OTP.");
            default -> model.addAttribute("errorMessage", "Có lỗi xảy ra, vui lòng thử lại.");
        }

        model.addAttribute("maskedEmail", maskEmail(email));
        return "verify-otp";
    }

    @PostMapping("/forgot-password/resend")
    public String resendOtp(HttpSession session, RedirectAttributes redirectAttributes) {
        String email = (String) session.getAttribute(SESSION_EMAIL);
        if (email == null) {
            return "redirect:/forgot-password";
        }

        boolean sent = otpService.generateAndSend(email, OtpPurpose.RESET_PASSWORD);
        redirectAttributes.addFlashAttribute(
                sent ? "successMessage" : "errorMessage",
                sent ? "Đã gửi lại mã OTP." : "Vui lòng đợi ít phút trước khi gửi lại OTP."
        );
        return "redirect:/forgot-password/verify";
    }

    // ===== BƯỚC 3: ĐẶT MẬT KHẨU MỚI =====
    @GetMapping("/reset-password")
    public String showResetPasswordForm(HttpSession session, Model model) {
        Boolean verified = (Boolean) session.getAttribute(SESSION_OTP_VERIFIED);
        if (verified == null || !verified) {
            return "redirect:/forgot-password";
        }
        model.addAttribute("resetPasswordRequest", new ResetPasswordRequest());
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String submitResetPassword(@Valid ResetPasswordRequest request, BindingResult bindingResult,
                                       HttpSession session, RedirectAttributes redirectAttributes) {

        Boolean verified = (Boolean) session.getAttribute(SESSION_OTP_VERIFIED);
        String email = (String) session.getAttribute(SESSION_EMAIL);

        if (verified == null || !verified || email == null) {
            return "redirect:/forgot-password";
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.confirmPassword", "Mật khẩu xác nhận không khớp");
        }

        if (bindingResult.hasErrors()) {
            return "reset-password";
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        session.removeAttribute(SESSION_EMAIL);
        session.removeAttribute(SESSION_OTP_VERIFIED);

        redirectAttributes.addFlashAttribute("successMessage", "Đặt lại mật khẩu thành công, vui lòng đăng nhập.");
        return "redirect:/login";
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return email;
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}