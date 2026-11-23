package com.zeqi.usermanagement.exception;

/**
 * 业务异常，携带要返回的 HTTP 状态码
 */
public class ApiException extends RuntimeException {

    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
