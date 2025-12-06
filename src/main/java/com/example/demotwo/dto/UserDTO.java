package com.example.demotwo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册/登录DTO
 * 含密码强度校验（企业级要求）
 */
@Data // Lombok自动生成get/set
public class UserDTO {
    // 用户名：非空 + 长度3-20
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需3-20字符")
    private String username;

    // 密码：非空 + 强度校验（必须包含大小写字母+数字，长度≥8）
    @NotBlank(message = "密码不能为空")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$",
        message = "密码需包含大小写字母和数字，长度≥8字符"
    )
    private String password;

    // 邮箱（可选，可加格式校验）
    @Pattern(
        regexp = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$",
        message = "邮箱格式错误"
    )
    private String email;

    // 角色（默认USER，管理员手动设置ADMIN）
    private String role = "USER";
}