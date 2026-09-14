package com.smartgrid.pricingservice.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class QuoteRedisService {

    private static final String PREFIX = "quote:";
    private static final String ACTIVE_SUFFIX = ":active";
    private static final String ACCEPT_LOCK_SUFFIX = ":accept-lock";
    private static final Duration ACCEPT_LOCK_SAFETY_TTL = Duration.ofDays(1);

    private final StringRedisTemplate redisTemplate;

    public QuoteRedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void markActive(UUID quoteId, Duration ttl) {
        redisTemplate.opsForValue().set(activeKey(quoteId), "1", ttl);
    }

    public boolean isActive(UUID quoteId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(activeKey(quoteId)));
    }

    public boolean tryAcceptLock(UUID quoteId) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(acceptLockKey(quoteId), "1", ACCEPT_LOCK_SAFETY_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    public static String activeKey(UUID quoteId) {
        return PREFIX + quoteId + ACTIVE_SUFFIX;
    }

    public static String acceptLockKey(UUID quoteId) {
        return PREFIX + quoteId + ACCEPT_LOCK_SUFFIX;
    }

    public static boolean isActiveKey(String redisKey) {
        return redisKey.startsWith(PREFIX) && redisKey.endsWith(ACTIVE_SUFFIX);
    }

    public static UUID quoteIdFromActiveKey(String redisKey) {
        return UUID.fromString(redisKey.substring(PREFIX.length(), redisKey.length() - ACTIVE_SUFFIX.length()));
    }
}
