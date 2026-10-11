package com.materin.tech.ops.common.exception;

import lombok.Getter;

/**
 * 业务异常：携带 code + message，由 GlobalExceptionHandler 统一转换为 Result。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
