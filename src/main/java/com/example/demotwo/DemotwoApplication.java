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
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;

// 排除数据源和R2DBC自动配置
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    R2dbcAutoConfiguration.class
})
public class DemotwoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemotwoApplication.class, args);
    }
}