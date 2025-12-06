package com.example.demotwo.entity;

import jakarta.persistence.*;
import lombok.Data; // 关键：Lombok生成setter/getter
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_portfolio")
@Data // 必须加！否则没有setUser/setCreateTime等方法
@DynamicInsert
@DynamicUpdate
public class Portfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String techStack;
    private String demoUrl;
    private String coverUrl;
    private LocalDateTime createTime; // 必须有这个字段，才能调用setCreateTime

    // 关联User（多对一）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // 数据库外键字段
    private User user; // 必须有这个字段，才能调用setUser
}