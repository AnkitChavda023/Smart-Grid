package com.smartgrid.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgrid.notificationservice.dto.NotificationPushMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisFanoutPublisher {

    public static final String CHANNEL = "notification-fanout";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisFanoutPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(NotificationPushMessage message) {
        try {
            redisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish fan-out message", e);
        }
    }
}
