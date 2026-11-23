package com.zeqi.usermanagement.service;

/**
 * 登录成功的结果
 */
public class LoginResult {

    private final String sessionId;
    private final String token;
    private final String username;

    public LoginResult(String sessionId, String token, String username) {
        this.sessionId = sessionId;
        this.token = token;
        this.username = username;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }
}
