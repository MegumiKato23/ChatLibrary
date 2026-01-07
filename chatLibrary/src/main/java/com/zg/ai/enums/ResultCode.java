package com.zg.ai.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * API 响应状态码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAILURE(400, "业务异常"),
    UNAUTHORIZED(401, "未授权"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误");
    
    private final int code;
    private final String message;
}
