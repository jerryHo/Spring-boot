package com.example.demotwo.exception;

import com.example.demotwo.common.Result; // 导入单独的Result类
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 全局异常处理器
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 处理自定义业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    // 处理参数校验异常
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleParamException(ConstraintViolationException e) {
        return Result.error(400, e.getMessage());
    }

    // 处理系统异常
    @ExceptionHandler(Exception.class)
    public Result<?> handleSystemException(Exception e) {
        return Result.error(500, "系统错误：" + e.getMessage());
    }
}