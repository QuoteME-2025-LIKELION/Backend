package com.ll.demo.domain.home.home.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Transactional(readOnly = true)
public class HomeController {
    @Value("${custom.site.name}")
    private String siteName;

    @GetMapping("/")
    @ResponseBody
    public String showMain() {
        return "Hello, World!, on " + siteName;
    }
}