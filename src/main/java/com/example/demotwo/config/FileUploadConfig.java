package com.example.demotwo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileUploadConfig {
    // 绑定application.properties中的file.upload.path属性
    @Value("${file.upload.path}")
    private String uploadPath;

    // 提供getter方法（后续文件上传时使用）
    public String getUploadPath() {
        return uploadPath;
    }
}