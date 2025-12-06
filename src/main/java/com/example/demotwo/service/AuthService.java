package com.example.demotwo.service;

import com.example.demotwo.dto.UserDTO;
import com.example.demotwo.entity.User;
import com.example.demotwo.repository.UserRepository;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 认证业务层
 * 核心：密码加密存储 + 登录密码验证
 */
@Service
public class AuthService {

    // 注入密码加密器（SecurityConfig中注册的Bean）
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    /**
     * 用户注册：核心是加密密码后保存
     */
    @Transactional(rollbackFor = Exception.class) // 事务保证数据一致性
    public User register(UserDTO userDTO) {
        // 1. 校验用户名是否已存在（避免重复）
        Optional<User> existUser = userRepository.findByUsername(userDTO.getUsername());
        if (existUser.isPresent()) {
            throw new RuntimeException("用户名已存在：" + userDTO.getUsername());
        }

        // 2. 密码加密（核心步骤！）
        String rawPassword = userDTO.getPassword(); // 前端传入的明文密码
        String encryptedPassword = passwordEncoder.encode(rawPassword); // BCrypt加密

        // 3. DTO转Entity（拷贝基础属性）
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        user.setPassword(encryptedPassword); // 替换为加密后的密码
        user.setCreateTime(LocalDateTime.now()); // 创建时间
        user.setUpdateTime(LocalDateTime.now()); // 更新时间

        // 4. 保存到数据库
        return userRepository.save(user);
    }

    /**
     * 用户登录：验证密码是否正确
     * @param username 用户名
     * @param rawPassword 前端传入的明文密码
     * @return 验证成功返回用户，失败返回null
     */
    public User login(String username, String rawPassword) {
        // 1. 查询用户是否存在
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return null; // 用户名不存在
        }
        User user = userOpt.get();

        // 2. 验证密码（核心！BCrypt自动匹配加密串和明文）
        boolean passwordMatch = passwordEncoder.matches(rawPassword, user.getPassword());
        if (!passwordMatch) {
            return null; // 密码错误
        }

        // 3. 验证通过，返回用户（实际项目中可生成JWT令牌）
        return user;
    }
}