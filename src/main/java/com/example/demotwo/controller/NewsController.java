package com.example.demotwo.controller;

import com.example.demotwo.util.NewsCrawler01;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for 香港01 News Crawler
 * 暴露API接口触发新闻爬取，支持提取完整新闻/链接/标题
 */
@Slf4j
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {
    // 修正类名：NewsCrawlera → NewsCrawler01（原工具类正确名称）
    private final NewsCrawler01 newsCrawler01;

    /**
     * 统一响应结构体
     */
    private static class ApiResponse<T> {
        private int code;          // 响应码（200成功，500失败）
        private String message;    // 响应信息
        private T data;            // 响应数据

        public ApiResponse(int code, String message, T data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }

        // Getter & Setter
        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
    }

    /**
     * 接口1：提取香港01完整新闻列表（含标题/类别/时间/URL/图片URL）
     * @return 结构化新闻数据
     */
    @GetMapping("/hk01/full")
    public ResponseEntity<ApiResponse<List<NewsCrawler01.NewsItem>>> crawlHk01FullNews() {
        try {
            log.info("开始调用接口：提取香港01完整新闻列表");
            List<NewsCrawler01.NewsItem> newsList = newsCrawler01.extractCompleteNewsList();
            
            return ResponseEntity.ok(
                new ApiResponse<>(200, "提取完整新闻成功", newsList)
            );
        } catch (Exception e) {
            log.error("提取完整新闻失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(500, "提取失败：" + e.getMessage(), null));
        }
    }

    /**
     * 接口2：提取香港01页面所有HTTP/HTTPS链接（去重）
     * @return 链接列表
     */
    @GetMapping("/hk01/links")
    public ResponseEntity<ApiResponse<List<String>>> crawlHk01AllLinks() {
        try {
            log.info("开始调用接口：提取香港01所有链接");
            List<String> linkList = newsCrawler01.extractAllLinks();
            
            return ResponseEntity.ok(
                new ApiResponse<>(200, "提取链接成功，共" + linkList.size() + "条", linkList)
            );
        } catch (Exception e) {
            log.error("提取链接失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(500, "提取失败：" + e.getMessage(), null));
        }
    }

    /**
     * 接口3：提取香港01所有中文标题（去重）
     * @return 标题列表
     */
    @GetMapping("/hk01/titles")
    public ResponseEntity<ApiResponse<List<String>>> crawlHk01ChineseTitles() {
        try {
            log.info("开始调用接口：提取香港01中文标题");
            List<String> titleList = newsCrawler01.extractChineseTitles();
            
            return ResponseEntity.ok(
                new ApiResponse<>(200, "提取标题成功，共" + titleList.size() + "条", titleList)
            );
        } catch (Exception e) {
            log.error("提取标题失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(500, "提取失败：" + e.getMessage(), null));
        }
    }

    /**
     * 兼容原接口：提取RTHK链接（保持原有URL路径，内部调用hk01链接提取）
     * 如需适配RTHK爬虫，可后续扩展NewsCrawler01增加extractRthkLinks方法
     */
    @GetMapping("/rthk")
    public ResponseEntity<ApiResponse<List<String>>> crawlRthk() {
        try {
            log.warn("兼容接口：/api/news/rthk 被调用，当前返回香港01链接（如需RTHK请扩展爬虫）");
            List<String> linkList = newsCrawler01.extractAllLinks();
            
            return ResponseEntity.ok(
                new ApiResponse<>(200, "提取RTHK链接（兼容模式）", linkList)
            );
        } catch (Exception e) {
            log.error("兼容接口提取失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(500, "提取失败：" + e.getMessage(), null));
        }
    }
}