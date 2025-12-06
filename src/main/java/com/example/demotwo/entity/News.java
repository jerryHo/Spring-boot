package com.example.demotwo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class News {
    // 新闻标题
    private String title;
    // 新闻链接
    private String url;
    // 新闻来源（如“香港01”）
    private String source;
    // 发布时间
    private String publishTime;
    // 抓取时间
    private LocalDateTime crawlTime;
    // 新闻摘要（提取正文前100字）
    private String summary;
}