package com.swp391.techforge.controller.authentication;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    /**
     * Hiển thị trang đăng nhập.
     * Logic kiểm tra đăng nhập (xác thực mật khẩu, kiểm tra trạng thái khóa) 
     * được xử lý ngầm bởi Spring Security chứ không nằm trong controller này.
     * 
     * @return Tên template trang đăng nhập (login.html)
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}