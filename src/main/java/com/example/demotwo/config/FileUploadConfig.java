// package com.example.demotwo.config;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
// import org.springframework.context.annotation.Configuration;

// // 关键：只有配置存在时才加载，否则跳过这个Bean
// @Configuration
// @ConditionalOnProperty(name = "file.upload.path", matchIfMissing = false)
// public class FileUploadConfig {
//     // 绑定application.properties中的file.upload.path属性
//     @Value("${file.upload.path}")
//     private String uploadPath;

//     // 提供getter方法（后续文件上传时使用）
//     public String getUploadPath() {
//         return uploadPath;
//     }
// }