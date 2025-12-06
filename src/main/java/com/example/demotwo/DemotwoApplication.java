// package com.example.demotwo;

// import org.springframework.boot.SpringApplication;
// import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration; // 新增
// import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration; // 新增

// // 确保扫描范围包含AuthController（比如controller子包）
// @SpringBootApplication(scanBasePackages = "com.example.demotwo")
// public class DemotwoApplication {
//     public static void main(String[] args) {
//         SpringApplication.run(DemotwoApplication.class, args);
//     }
// }

// 新增包声明（关键！）
package com.example.demotwo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories; // 关键：启用JPA仓库扫描

// 1. 显式指定JPA仓库扫描路径（解决Repository Bean缺失）
// 2. 确保扫描范围包含所有业务包
@SpringBootApplication(scanBasePackages = "com.example.demotwo")
@EnableJpaRepositories(basePackages = "com.example.demotwo.repository") // 强制扫描Repository包
public class DemotwoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemotwoApplication.class, args);
    }
}