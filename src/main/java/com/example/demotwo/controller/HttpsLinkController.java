// HttpsLinkController.java (controller package)
package com.example.demotwo.controller;

import com.example.demotwo.util.NewsCrawlera;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// HttpsLinkController.java
@RestController
@RequestMapping("/api/https-links")
@RequiredArgsConstructor
public class HttpsLinkController {
    private final NewsCrawlera newsCrawlera;

    @GetMapping("/rthk")
    public List<String> getAllRthkHttpsLinks() {
        // 改为NewsCrawlera中实际存在的方法名
        return newsCrawlera.extractAllLinks();
    }
}