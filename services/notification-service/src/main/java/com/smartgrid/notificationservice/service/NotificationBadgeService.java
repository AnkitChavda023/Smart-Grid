package com.smartgrid.notificationservice.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationBadgeService {

    private final StringRedisTemplate redisTemplate;

    public NotificationBadgeService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void increment(String userId) {
        redisTemplate.opsForValue().increment(key(userId));
    }

    public long current(String userId) {
        String value = redisTemplate.opsForValue().get(key(userId));
        return value == null ? 0 : Long.parseLong(value);
    }

    public void clear(String userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(String userId) {
        return "notif:badge:" + userId;
    }
}
