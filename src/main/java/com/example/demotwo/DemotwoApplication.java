package com.example.demotwo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration; // 新增
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration; // 新增

// 确保扫描范围包含AuthController（比如controller子包）
@SpringBootApplication(scanBasePackages = "com.example.demotwo")
public class DemotwoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemotwoApplication.class, args);
    }
}