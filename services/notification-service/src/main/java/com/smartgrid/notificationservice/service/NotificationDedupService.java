package com.smartgrid.notificationservice.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class NotificationDedupService {

    private static final Duration DEDUP_TTL = Duration.ofDays(1);

    private final StringRedisTemplate redisTemplate;

    public NotificationDedupService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // SET NX + TTL in one atomic call: the first caller for a given (eventId, userId) pair gets true and
    // proceeds; every subsequent delivery attempt for the same pair gets false and is dropped as a duplicate.
    public boolean tryClaim(String eventId, String userId) {
        String key = "notif:dedup:" + eventId + ":" + userId;
        Boolean firstClaim = redisTemplate.opsForValue().setIfAbsent(key, "1", DEDUP_TTL);
        return Boolean.TRUE.equals(firstClaim);
    }
}
