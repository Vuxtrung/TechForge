package com.swp391.techforge.controller.home;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/home")
    public String redirectHome() {
        return "redirect:/";
    }

    @GetMapping("/buildpc")
    public String buildpc(org.springframework.ui.Model model) {
        model.addAttribute("pageTitle", "Tự Build PC");
        return "buildpc";
    }
}