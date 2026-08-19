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

    @ExceptionHandler(Exception.class)
    public void handleAllExceptions(Exception ex) throws Exception {
        try {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            ex.printStackTrace(pw);
            String stackTrace = sw.toString();
            
            java.nio.file.Files.write(
                java.nio.file.Paths.get("C:/Users/admin/Desktop/SWP391/TechForge/TechForge/error_debug.log"), 
                (stackTrace + "\n====================\n").getBytes(), 
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw ex; // Re-throw to not break default handling
    }
}