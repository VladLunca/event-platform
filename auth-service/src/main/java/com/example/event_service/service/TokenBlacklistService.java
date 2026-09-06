package com.example.event_service.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Token-uri revocate prin logout, tinute in Redis.
 * Fiecare intrare primeste un TTL egal cu viata ramasa a token-ului, deci se sterge singura
 * exact cand token-ul ar fi expirat oricum. Fiind in afara procesului, blacklist-ul e partajat
 * intre replicile de auth-service si supravietuieste unui restart.
 */
@Service
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "bl:";

    private final StringRedisTemplate redis;
    private final JwtService jwtService;

    public TokenBlacklistService(StringRedisTemplate redis, JwtService jwtService) {
        this.redis = redis;
        this.jwtService = jwtService;
    }

    public void add(String token) {
        long ttlMs = jwtService.getExpiration(token) - System.currentTimeMillis();
        if (ttlMs <= 0) {
            return;
        }
        redis.opsForValue().set(key(token), "1", Duration.ofMillis(ttlMs));
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redis.hasKey(key(token)));
    }

    private String key(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return KEY_PREFIX + HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponibil", e);
        }
    }
}
