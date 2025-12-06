package com.example.demotwo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "sys_upload_file")
@Data
public class UploadFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 原文件名
    @Column(nullable = false)
    private String originalFileName;

    // 存储文件名（避免重复）
    @Column(nullable = false)
    private String storeFileName;

    // 文件大小（字节）
    private Long fileSize;

    // 文件类型（如doc/xlsx/png）
    private String fileType;

    // 存储路径
    @Column(nullable = false)
    private String filePath;

    // 关联用户
    @Column(nullable = false)
    private String username;

    // 上传时间
    private LocalDateTime uploadTime;

    @PrePersist
    public void prePersist() {
        this.uploadTime = LocalDateTime.now();
    }
}