package com.example.demotwo.controller;

import com.example.demotwo.util.NewsCrawlera;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/titles")
@RequiredArgsConstructor
public class TitleController {

    private final NewsCrawlera newsCrawlera;

    /**
     * API接口：返回目标页面的所有中文标题
     */
    @GetMapping("/chinese")
    public List<String> getChineseTitles() {
        return newsCrawlera.extractChineseTitles();
    }
}