package com.example.demotwo.repository;

import com.example.demotwo.entity.UploadFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FileRepository extends JpaRepository<UploadFile, Long> {
    // 查询用户的所有文件
    List<UploadFile> findByUsernameOrderByUploadTimeDesc(String username);
    // 按文件类型统计数量
    long countByUsernameAndFileTypeContaining(String username, String type);
}