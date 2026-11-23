package com.zeqi.usermanagement.service;

import com.zeqi.usermanagement.dto.LoginRequest;
import com.zeqi.usermanagement.dto.PasswordRequest;
import com.zeqi.usermanagement.dto.RegisterRequest;
import com.zeqi.usermanagement.dto.RenameRequest;
import com.zeqi.usermanagement.entity.User;
import com.zeqi.usermanagement.exception.ApiException;
import com.zeqi.usermanagement.repository.UserRepository;
import com.zeqi.usermanagement.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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

    /** 第四题的登录态：sessionId -> userId */
    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    public UserService(UserRepository userRepository, BCryptPasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    /** 注册 */
    public User register(RegisterRequest request) {
        String username = request.getUsername().trim();

        if (userRepository.existsByUserName(username)) {
            throw new ApiException(409, "用户名已存在");
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
        return user;
    }

    /** 登录，返回 sessionId 和 JWT */
    public LoginResult login(LoginRequest request) {
        User user = userRepository.findByUserName(request.getUsername());

        if (user == null || !encoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("用户登录失败 username={}", request.getUsername());
            throw new ApiException(401, "用户名或密码错误");
        }

        String sessionId = UUID.randomUUID().toString().replace("-", "");
        sessions.put(sessionId, user.getUserId());

        log.info("用户登录成功 userId={} username={}", user.getUserId(), user.getUserName());
        return new LoginResult(sessionId, JwtUtil.generateToken(user.getUserId()), user.getUserName());
    }

    /** 按 JWT 或 sessionId 取当前用户 */
    public User requireUser(String jwtUserId, String sessionId) {
        String userId = jwtUserId;

        if (userId == null && sessionId != null) {
            userId = sessions.get(sessionId);
        }
        if (userId == null) {
            throw new ApiException(401, "未登录");
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "用户不存在"));
    }

    /** 用户列表 */
    public List<User> listUsers(String jwtUserId, String sessionId) {
        requireUser(jwtUserId, sessionId);
        return userRepository.findAll();
    }

    /** 修改用户名，改成自己原来的名字是合法的 */
    public User rename(String jwtUserId, String sessionId, RenameRequest request) {
        User user = requireUser(jwtUserId, sessionId);
        String newName = request.getNewUsername().trim();

        if (!newName.equals(user.getUserName()) && userRepository.existsByUserName(newName)) {
            throw new ApiException(409, "用户名已存在");
        }

        user.setUserName(newName);
        userRepository.save(user);

        log.info("修改用户名成功 userId={} userName={}", user.getUserId(), user.getUserName());
        return user;
    }

    /** 修改密码，需要校验原密码 */
    public void changePassword(String jwtUserId, String sessionId, PasswordRequest request) {
        User user = requireUser(jwtUserId, sessionId);

        if (!encoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new ApiException(400, "原密码错误");
        }

        user.setPasswordHash(encoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("修改密码成功 userId={}", user.getUserId());
    }

    /** 注销，同时清掉 session */
    public void delete(String jwtUserId, String sessionId) {
        User user = requireUser(jwtUserId, sessionId);

        userRepository.deleteById(user.getUserId());
        if (sessionId != null) {
            sessions.remove(sessionId);
        }

        log.info("用户注销 userId={}", user.getUserId());
    }
}
