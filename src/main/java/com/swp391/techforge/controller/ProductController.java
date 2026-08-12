package com.swp391.techforge.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProductController {

    @GetMapping("/productlist")
    public String productlist() {
        return "productlist";
    }

    @GetMapping("/productdetail")
    public String productdetail() {
        return "productdetail";
    }
}
