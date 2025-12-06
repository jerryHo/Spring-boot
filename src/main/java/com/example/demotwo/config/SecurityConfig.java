package com.example.demotwo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
// 关键：导入antMatcher静态方法（6.x推荐写法）
import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 解决AuthController依赖的PasswordEncoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. 关闭CSRF（开发环境简化）
            .csrf(AbstractHttpConfigurer::disable)
            // 2. 配置权限规则（6.x合规写法，用antMatcher静态方法）
            .authorizeHttpRequests(auth -> auth
                // 放行根目录的news-list.html（精准匹配）
                .requestMatchers(antMatcher("/news-list.html")).permitAll()
                // 放行所有静态资源（CSS/JS/ICO，Ant风格通配符）
                .requestMatchers(antMatcher("/**/*.css")).permitAll()
                .requestMatchers(antMatcher("/**/*.js")).permitAll()
                .requestMatchers(antMatcher("/**/*.ico")).permitAll()
                // 放行新闻API接口
                .requestMatchers(antMatcher("/api/news/**")).permitAll()
                // 其他所有请求放行（开发环境）
                .anyRequest().permitAll()
            );

        return http.build();
    }
}