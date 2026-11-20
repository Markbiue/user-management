package com.zeqi.usermanagement.interceptor;

import com.zeqi.usermanagement.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

// 拦截非法 JWT
public class JwtInterceptor implements HandlerInterceptor{
    @Override
    public boolean preHandle(HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {

        // 取出请求头 Authorization
        String auth =  request.getHeader("Authorization");

        // 由于第四题请求头里面没有Authorization，所以用 Controller 通过 sessionId 判断
        if (auth == null) {
            return true;
        }

        if (!auth.startsWith("Bearer ")) {
            return reject(response);
        }

        // 去除 Bearer，剩下 Token
        String token = auth.substring(7);

        // 校验 Token
        String userId = JwtUtil.parse(token);
        if(userId == null){
            return reject(response);
        }

        // 将userId 放入request
        request.setAttribute("userId", userId);

        return true;
    }
    private boolean reject(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"未登录或登录已过期\"}");
        return false;
    }
}
