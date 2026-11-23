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
import java.util.Optional;

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
        Optional<User> created = userService.register(request);
        if (created.isEmpty()) {
            return ResponseEntity.status(409).body(Map.of("message", "用户名已存在"));
        }

        User user = created.get();
        return ResponseEntity.status(201).body(Map.of(
                "id", user.getUserId(),
                "username", user.getUserName()
        ));
    }

    @PostMapping("/sessions")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Optional<LoginResult> login = userService.login(request);
        if (login.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "用户名或密码错误"));
        }

        LoginResult result = login.get();
        return ResponseEntity.ok(Map.of(
                "sessionId", result.getSessionId(),
                "token", result.getToken(),
                "username", result.getUsername()
        ));
    }

    @GetMapping("/users/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        Optional<User> current = userService.currentUser(jwtUserId(request), sessionId(request));
        if (current.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        }

        User user = current.get();
        return ResponseEntity.ok(Map.of(
                "id", user.getUserId(),
                "username", user.getUserName(),
                "email", user.getEmail()
        ));
    }

    @GetMapping("/users")
    public ResponseEntity<?> list(HttpServletRequest request) {
        Optional<User> current = userService.currentUser(jwtUserId(request), sessionId(request));
        if (current.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (User user : userService.findAll()) {
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
        Optional<User> current = userService.currentUser(jwtUserId(request), sessionId(request));
        if (current.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        }

        Optional<User> renamed = userService.rename(current.get(), body);
        if (renamed.isEmpty()) {
            return ResponseEntity.status(409).body(Map.of("message", "用户名已存在"));
        }

        User user = renamed.get();
        return ResponseEntity.ok(Map.of(
                "id", user.getUserId(),
                "username", user.getUserName()
        ));
    }

    @PutMapping("/users/me/password")
    public ResponseEntity<?> changePassword(HttpServletRequest request,
                                            @Valid @RequestBody PasswordRequest body) {
        Optional<User> current = userService.currentUser(jwtUserId(request), sessionId(request));
        if (current.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        }

        boolean changed = userService.changePassword(current.get(), body);
        if (!changed) {
            return ResponseEntity.badRequest().body(Map.of("message", "原密码错误"));
        }

        return ResponseEntity.ok(Map.of("message", "密码修改成功"));
    }

    @DeleteMapping("/users/me")
    public ResponseEntity<?> delete(HttpServletRequest request) {
        Optional<User> current = userService.currentUser(jwtUserId(request), sessionId(request));
        if (current.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        }

        userService.delete(current.get(), sessionId(request));
        return ResponseEntity.noContent().build();
    }

    private String jwtUserId(HttpServletRequest request) {
        return (String) request.getAttribute("userId");
    }

    private String sessionId(HttpServletRequest request) {
        return request.getHeader("sessionId");
    }
}
