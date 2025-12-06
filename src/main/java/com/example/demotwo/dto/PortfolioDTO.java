package com.example.demotwo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 作品集请求参数DTO
 * 含JSR-380参数校验注解
 */
@Data // Lombok 自动生成get/set/toString
public class PortfolioDTO {

    @NotBlank(message = "作品集标题不能为空")
    @Size(max = 100, message = "标题长度不能超过100字符")
    private String title;

    @Size(max = 500, message = "描述长度不能超过500字符")
    private String description;

    @NotBlank(message = "技术栈不能为空")
    private String techStack;

    @Size(max = 200, message = "演示链接长度不能超过200字符")
    private String demoUrl;

    @Size(max = 200, message = "封面图链接长度不能超过200字符")
    private String coverUrl;
}