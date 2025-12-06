package com.example.demotwo.common;

import lombok.Data;

// 统一响应结果类（所有接口返回这个格式）
@Data
public class Result<T> {
    private int code; // 响应码：200成功，400参数错误，500系统错误
    private String msg; // 响应消息
    private T data; // 响应数据

    // 成功响应（带数据）
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    // 失败响应（带错误码和消息）
    public static <T> Result<T> error(int code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(null);
        return result;
    }
}