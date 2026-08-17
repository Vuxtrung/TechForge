package com.swp391.techforge.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Quản trị hệ thống");
        return "admin/dashboard";
    }
}
