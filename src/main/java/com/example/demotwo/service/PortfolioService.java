package com.example.demotwo.service;

// 修复Repository导入（对应步骤2创建的包）
import com.example.demotwo.dto.PortfolioDTO;
import com.example.demotwo.entity.Portfolio;
import com.example.demotwo.entity.User;
import com.example.demotwo.repository.PortfolioRepository;
import com.example.demotwo.repository.UserRepository;

// 修复Spring Data JPA导入（对应步骤1的依赖）
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

// 修复数据校验导入
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import lombok.RequiredArgsConstructor;

// 修复StringUtils导入（二选一）
import org.springframework.util.StringUtils; // 推荐：Spring内置，无需额外依赖
// import org.apache.commons.lang3.StringUtils; // 若用Apache的，需步骤1的依赖

import java.time.LocalDateTime;
import java.util.Set;


/**
 * 作品集业务层
 * 处理作品集的新增、查询等核心业务逻辑
 */
@Service
@RequiredArgsConstructor // Lombok自动生成构造器注入
public class PortfolioService {

    // 自动注入数据访问层
    @Autowired
    private PortfolioRepository portfolioRepository;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * 新增作品集（含数据校验 + 用户关联）
     * @param portfolioDTO 作品集请求参数DTO
     * @param username 当前登录用户名
     * @return 保存后的作品集实体
     */
    public Portfolio addPortfolio(PortfolioDTO portfolioDTO, String username) {
        // 1. 数据校验（基于JSR-380注解）
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Set<ConstraintViolation<PortfolioDTO>> violations = factory.getValidator().validate(portfolioDTO);
        if (!violations.isEmpty()) {
            // 抛出参数校验异常，返回第一条错误信息
            throw new IllegalArgumentException("参数错误：" + violations.iterator().next().getMessage());
        }

        // 2. 根据用户名查询关联用户（不存在则抛异常）
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在：" + username));

        // 3. DTO 转换为 Entity（属性拷贝）
        Portfolio portfolio = new Portfolio();
        BeanUtils.copyProperties(portfolioDTO, portfolio); // 拷贝基础属性
        portfolio.setUser(user); // 关联用户
        portfolio.setCreateTime(LocalDateTime.now()); // 设置创建时间

        // 4. 保存到数据库并返回
        return portfolioRepository.save(portfolio);
    }

    /**
     * 分页查询作品集（支持按技术栈筛选）
     * @param page 页码（前端从1开始）
     * @param size 每页条数
     * @param techStack 技术栈筛选关键词（可为空）
     * @return 分页后的作品集列表
     */
    public Page<Portfolio> getPortfolios(Integer page, Integer size, String techStack) {
        // 构建分页参数：页码转换（JPA从0开始）+ 按创建时间降序排序
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createTime").descending());
        
        // 按技术栈筛选或查询全部
        if (StringUtils.hasText(techStack)) {
            return portfolioRepository.findByTechStackContaining(techStack, pageable);
        }
        return portfolioRepository.findAll(pageable);
    }
}

