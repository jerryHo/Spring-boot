package com.example.demotwo.repository;

import com.example.demotwo.entity.Portfolio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    // 按技术栈模糊查询（与Service中的findByTechStackContaining对应）
    Page<Portfolio> findByTechStackContaining(String techStack, Pageable pageable);
}