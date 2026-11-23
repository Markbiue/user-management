package com.zeqi.usermanagement.interceptor;

import com.zeqi.usermanagement.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 校验 Authorization 里的 JWT，校验通过把 userId 放进 request
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String auth = request.getHeader("Authorization");

        // 没带 Authorization，交给 Controller 用 sessionId 判断
        if (auth == null) {
            return true;
        }
        if (!auth.startsWith("Bearer ")) {
            return reject(response);
        }

        String token = auth.substring(7);
        String userId = jwtUtil.parse(token);
        if (userId == null) {
            return reject(response);
        }

        request.setAttribute("userId", userId);
        return true;
    }

    private boolean reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"未登录或登录已过期\"}");
        return false;
    }
}
