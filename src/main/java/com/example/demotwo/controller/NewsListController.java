package com.example.demotwo.controller;

import com.example.demotwo.util.NewsCrawlera;
import com.example.demotwo.util.NewsCrawlera.NewsItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsListController {

    private final NewsCrawlera newsCrawlera;

    /**
     * 接口1：返回JSON格式的新闻列表（供接口调用）
     */
    @GetMapping("/list")
    @ResponseBody
    public List<NewsItem> getCompleteNewsList() {
        return newsCrawlera.extractCompleteNewsList();
    }

    /**
     * 接口2：返回网页，展示新闻列表（前端展示用）
     */
    @GetMapping("/page")
    public String getNewsListPage(Model model) {
        // 获取新闻列表数据并传入页面
        List<NewsItem> newsList = newsCrawlera.extractCompleteNewsList();
        model.addAttribute("newsList", newsList);
        model.addAttribute("pageTitle", "香港电台新闻列表 | 2025-12-05");
        // 返回Thymeleaf模板名称（对应src/main/resources/templates/news-list.html）
        return "news-list";
    }
}