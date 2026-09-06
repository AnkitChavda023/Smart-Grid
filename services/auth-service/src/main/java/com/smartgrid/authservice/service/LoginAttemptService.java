package com.smartgrid.authservice.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class LoginAttemptService {

    private static final String FAILURE_COUNT_PREFIX = "login:fail:";
    private static final String LOCKOUT_PREFIX = "login:lock:";

    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);
    private static final int BACKOFF_THRESHOLD = 3;
    private static final Duration BACKOFF_BASE = Duration.ofSeconds(2);
    private static final Duration BACKOFF_MAX = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    public LoginAttemptService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<Duration> lockoutRemaining(String username) {
        Long ttlSeconds = redisTemplate.getExpire(LOCKOUT_PREFIX + username);
        if (ttlSeconds == null || ttlSeconds <= 0) {
            return Optional.empty();
        }
        return Optional.of(Duration.ofSeconds(ttlSeconds));
    }

    public void recordFailure(String username) {
        String failureKey = FAILURE_COUNT_PREFIX + username;
        Long failureCount = redisTemplate.opsForValue().increment(failureKey);
        if (failureCount != null && failureCount == 1) {
            redisTemplate.expire(failureKey, FAILURE_WINDOW);
        }

        if (failureCount != null && failureCount >= BACKOFF_THRESHOLD) {
            long backoffSeconds = Math.min(
                    BACKOFF_MAX.getSeconds(),
                    BACKOFF_BASE.getSeconds() * (1L << (failureCount - BACKOFF_THRESHOLD))
            );
            redisTemplate.opsForValue().set(LOCKOUT_PREFIX + username, "1", Duration.ofSeconds(backoffSeconds));
        }
    }

    public void recordSuccess(String username) {
        redisTemplate.delete(FAILURE_COUNT_PREFIX + username);
        redisTemplate.delete(LOCKOUT_PREFIX + username);
    }
}
