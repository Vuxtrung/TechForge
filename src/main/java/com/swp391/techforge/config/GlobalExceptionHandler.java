package com.swp391.techforge.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(HttpServletRequest request,
                                       RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage",
                "Ảnh tải lên vượt quá dung lượng cho phép (tối đa 5MB). Vui lòng chọn ảnh khác.");

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/admin/products");
    }
}