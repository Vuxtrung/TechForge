package com.swp391.techforge.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BuildPcController {
    @GetMapping("/buildpc")
    public String buildpc() {
        return "buildpc";
    }
}
