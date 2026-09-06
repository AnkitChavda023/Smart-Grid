package com.smartgrid.authservice.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenStore {

    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);
    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redisTemplate;

    public RefreshTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Duration ttl() {
        return REFRESH_TOKEN_TTL;
    }

    public String issue(String username) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(KEY_PREFIX + token, username, REFRESH_TOKEN_TTL);
        return token;
    }

    public Optional<String> resolveUsername(String refreshToken) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + refreshToken));
    }

    public void revoke(String refreshToken) {
        redisTemplate.delete(KEY_PREFIX + refreshToken);
    }
}
