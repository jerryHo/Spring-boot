package com.example.demotwo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/")
public class HomeController {

    // 🌟 Forwards root path to index.html (your home page)
    @GetMapping("/")
    public String redirectToHomePage() {
        return "forward:/index.html";
    }

    // 🌟 Your original endpoint (unchanged)
    @GetMapping("/indexx")
    @ResponseBody
    public String helloIndex() {
        return "index";
    }

    // 🌟 NEW: Dedicated API endpoint for health check (JS will fetch this)
    @GetMapping("/api/health")
    @ResponseBody
    public String healthCheck() {
        return "OK"; // Simple response (no HTML)
    }
}