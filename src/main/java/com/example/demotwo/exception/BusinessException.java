package com.example.demotwo.exception;

// 自定义业务异常（用于业务逻辑错误）
public class BusinessException extends RuntimeException {
    private int code; // 错误码（如400、500）

    // 构造方法
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    // Getter方法
    public int getCode() {
        return code;
    }
}