package com.zeqi.usermanagement.controller;

import com.zeqi.usermanagement.entity.User;
import com.zeqi.usermanagement.repository.UserRepository;
import com.zeqi.usermanagement.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final Map<String, String> sessions = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/users")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String email = body.get("email");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名和密码不能为空"));
        }
        if (userRepository.existsByUserName(username)) {
            return ResponseEntity.status(409).body(Map.of("message", "用户名已存在"));
        }
        if (email == null || email.isBlank()) {
            email = username + "@local";
        }


        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setUserName(username);
        user.setPasswordHash(encoder.encode(password));
        user.setEmail(email);
        userRepository.save(user);
        log.info("用户注册成功 userId={} username={}", user.getUserId(), username);

        return ResponseEntity.status(201).body(Map.of(
                "id", user.getUserId(),
                "username", user.getUserName()
        ));
    }

    private User currentUser(HttpServletRequest request) {
        // 优先使用 JWT，没有则回退到 sessionId

        String userId = (String)request.getAttribute("userId");
        // 没有 Authorization，没拿到Id
        if (userId == null) {
            String sessionId = request.getHeader("sessionId");
            if(sessionId != null) {
                userId = sessions.get(sessionId);
            }
        }
        if (userId == null) {
            return null;
        }

        return userRepository.findById(userId).orElse(null);
    }

    @PostMapping("/sessions")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        User user = userRepository.findByUserName(username);

        if (user == null || !encoder.matches(password, user.getPasswordHash())) {
            log.warn("用户登录失败 username={}", username);
            return ResponseEntity.status(401).body(Map.of("message", "用户名或密码错误"));
        }

        String sessionId = UUID.randomUUID().toString().replace("-", "");
        sessions.put(sessionId, user.getUserId());
        log.info("用户登录成功 userId={} username={}", user.getUserId(), username);

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "token", JwtUtil.generateToken(user.getUserId()),
                "username", user.getUserName()
        ));
    }

    @GetMapping("/users/me")
    public ResponseEntity<?> me(HttpServletRequest request) {

        User user = currentUser(request);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        }

        return ResponseEntity.ok(Map.of(
                "id", user.getUserId(),
                "username", user.getUserName(),
                "email", user.getEmail()
        ));
    }

    @GetMapping("/users")
    public ResponseEntity<?> list(HttpServletRequest request) {

        if (currentUser(request) == null) {
            return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : userRepository.findAll()) {
            result.add(Map.of(
                    "id", u.getUserId(),
                    "username", u.getUserName(),
                    "email", u.getEmail()
            ));
        }
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/users/me")
    public ResponseEntity<?> rename(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {

        User user = currentUser(request);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        }

        String newName = body.get("newUsername");
        if (newName == null || newName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "新用户名不能为空"));
        }
        if (!newName.equals(user.getUserName())
                && userRepository.existsByUserName(newName)) {
            return ResponseEntity.status(409).body(Map.of("message", "用户名已存在"));
        }

        user.setUserName(newName);
        userRepository.save(user);
        log.info("修改用户名成功 userId={} userName={}",user.getUserId(), user.getUserName());

        return ResponseEntity.ok(Map.of("id", user.getUserId(), "username", user.getUserName()));
    }

    @PutMapping("/users/me/password")
    public ResponseEntity<?> changePassword(
            HttpServletRequest request,
            @RequestBody Map<String, String> body) {

        User user = currentUser(request);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        }

        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        if (oldPassword == null || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "密码不能为空"));
        }
        if (!encoder.matches(oldPassword, user.getPasswordHash())) {
            return ResponseEntity.badRequest().body(Map.of("message", "原密码错误"));
        }

        user.setPasswordHash(encoder.encode(newPassword));
        userRepository.save(user);
        log.info("修改密码成功 userId={}", user.getUserId());

        return ResponseEntity.ok(Map.of("message", "密码修改成功"));
    }

    @DeleteMapping("/users/me")
    public ResponseEntity<?> delete(
            HttpServletRequest request) {

        User user = currentUser(request);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "未登录"));
        }

        userRepository.deleteById(user.getUserId());

        String sessionId = request.getHeader("sessionId");
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
        log.info("用户注销 userId={}", user.getUserId());

        return ResponseEntity.noContent().build();
    }
}
