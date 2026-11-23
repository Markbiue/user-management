package com.zeqi.usermanagement.controller;

import com.zeqi.usermanagement.dto.LoginRequest;
import com.zeqi.usermanagement.dto.PasswordRequest;
import com.zeqi.usermanagement.dto.RegisterRequest;
import com.zeqi.usermanagement.dto.RenameRequest;
import com.zeqi.usermanagement.entity.User;
import com.zeqi.usermanagement.service.LoginResult;
import com.zeqi.usermanagement.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 只负责接收请求、调用 Service、组装响应
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return ResponseEntity.status(201).body(Map.of(
                "id", user.getUserId(),
                "username", user.getUserName()
        ));
    }

    @PostMapping("/sessions")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = userService.login(request);
        return ResponseEntity.ok(Map.of(
                "sessionId", result.getSessionId(),
                "token", result.getToken(),
                "username", result.getUsername()
        ));
    }

    @GetMapping("/users/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        User user = userService.requireUser(jwtUserId(request), sessionId(request));
        return ResponseEntity.ok(Map.of(
                "id", user.getUserId(),
                "username", user.getUserName(),
                "email", user.getEmail()
        ));
    }

    @GetMapping("/users")
    public ResponseEntity<?> list(HttpServletRequest request) {
        List<User> users = userService.listUsers(jwtUserId(request), sessionId(request));

        List<Map<String, Object>> result = new ArrayList<>();
        for (User user : users) {
            result.add(Map.of(
                    "id", user.getUserId(),
                    "username", user.getUserName(),
                    "email", user.getEmail()
            ));
        }
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/users/me")
    public ResponseEntity<?> rename(HttpServletRequest request,
                                    @Valid @RequestBody RenameRequest body) {
        User user = userService.rename(jwtUserId(request), sessionId(request), body);
        return ResponseEntity.ok(Map.of(
                "id", user.getUserId(),
                "username", user.getUserName()
        ));
    }

    @PutMapping("/users/me/password")
    public ResponseEntity<?> changePassword(HttpServletRequest request,
                                            @Valid @RequestBody PasswordRequest body) {
        userService.changePassword(jwtUserId(request), sessionId(request), body);
        return ResponseEntity.ok(Map.of("message", "密码修改成功"));
    }

    @DeleteMapping("/users/me")
    public ResponseEntity<?> delete(HttpServletRequest request) {
        userService.delete(jwtUserId(request), sessionId(request));
        return ResponseEntity.noContent().build();
    }

    private String jwtUserId(HttpServletRequest request) {
        return (String) request.getAttribute("userId");
    }

    private String sessionId(HttpServletRequest request) {
        return request.getHeader("sessionId");
    }
}
