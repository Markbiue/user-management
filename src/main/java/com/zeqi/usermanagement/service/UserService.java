package com.zeqi.usermanagement.service;

import com.zeqi.usermanagement.dto.LoginRequest;
import com.zeqi.usermanagement.dto.PasswordRequest;
import com.zeqi.usermanagement.dto.RegisterRequest;
import com.zeqi.usermanagement.dto.RenameRequest;
import com.zeqi.usermanagement.entity.User;
import com.zeqi.usermanagement.repository.UserRepository;
import com.zeqi.usermanagement.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户业务逻辑：注册、登录、登录态、改名、改密、注销
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    /** 第四题的登录态：sessionId -> userId */
    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    public UserService(UserRepository userRepository, BCryptPasswordEncoder encoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    /** 注册，用户名已存在时返回空 */
    public Optional<User> register(RegisterRequest request) {
        String username = request.getUsername().trim();

        if (userRepository.existsByUserName(username)) {
            return Optional.empty();
        }

        String email = request.getEmail();
        if (email == null || email.isBlank()) {
            email = username + "@local";
        }

        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setUserName(username);
        user.setPasswordHash(encoder.encode(request.getPassword()));
        user.setEmail(email);
        userRepository.save(user);

        log.info("用户注册成功 userId={} username={}", user.getUserId(), username);
        return Optional.of(user);
    }

    /** 登录，用户名或密码错误时返回空 */
    public Optional<LoginResult> login(LoginRequest request) {
        User user = userRepository.findByUserName(request.getUsername());

        if (user == null || !encoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("用户登录失败 username={}", request.getUsername());
            return Optional.empty();
        }

        String sessionId = UUID.randomUUID().toString().replace("-", "");
        sessions.put(sessionId, user.getUserId());

        log.info("用户登录成功 userId={} username={}", user.getUserId(), user.getUserName());
        return Optional.of(new LoginResult(sessionId, jwtUtil.generateToken(user.getUserId()), user.getUserName()));
    }

    /** 按 JWT 或 sessionId 取当前用户，未登录返回空 */
    public Optional<User> currentUser(String jwtUserId, String sessionId) {
        String userId = jwtUserId;

        if (userId == null && sessionId != null) {
            userId = sessions.get(sessionId);
        }
        if (userId == null) {
            return Optional.empty();
        }

        return userRepository.findById(userId);
    }

    /** 用户列表 */
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /** 修改用户名，新用户名被别人占用时返回空 */
    public Optional<User> rename(User user, RenameRequest request) {
        String newName = request.getNewUsername().trim();

        if (!newName.equals(user.getUserName()) && userRepository.existsByUserName(newName)) {
            return Optional.empty();
        }

        user.setUserName(newName);
        userRepository.save(user);

        log.info("修改用户名成功 userId={} userName={}", user.getUserId(), user.getUserName());
        return Optional.of(user);
    }

    /** 修改密码，原密码错误时返回 false */
    public boolean changePassword(User user, PasswordRequest request) {
        if (!encoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            return false;
        }

        user.setPasswordHash(encoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("修改密码成功 userId={}", user.getUserId());
        return true;
    }

    /** 注销，同时清掉 session */
    public void delete(User user, String sessionId) {
        userRepository.deleteById(user.getUserId());

        if (sessionId != null) {
            sessions.remove(sessionId);
        }

        log.info("用户注销 userId={}", user.getUserId());
    }
}
