package com.zeqi.usermanagement.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

//生成 JWT Token
public class JwtUtil {

    // 生成密钥
    private static final String SECRET = "zeqi-user-management-secret-key-2026";

    // 将密钥转化成 key
    private static final SecretKey KEY =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    // 设置有效期: 一天
    private static final long EXPIRATION_TIME = 24*60*60*1000;

    // 生成 Token
    public static String generateToken(String userId){
        Date now = new Date();

        return Jwts.builder()
                // Payload :用户Id
                .subject(userId)
                // 签发时间
                .issuedAt(now)
                // 过期时间
                .expiration(new Date(now.getTime() + EXPIRATION_TIME))
                // Signature :密钥签名
                .signWith(KEY)
                // 拼成字符串
                .compact();
    }

    // 校验 Token
    public static String parse(String token){
        try{
            Claims claims = Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getSubject();
        }catch (JwtException | IllegalArgumentException e) {
            // 签名不对、格式错误,过期
            return null;
        }
    }

}
