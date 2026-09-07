package com.smartgrid.notificationservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgrid.notificationservice.dto.NotificationPushMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class RedisFanoutRelay implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public RedisFanoutRelay(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            NotificationPushMessage push = objectMapper.readValue(
                    new String(message.getBody(), StandardCharsets.UTF_8), NotificationPushMessage.class);

            // Always broadcast to global notification topic so bell badge and notification drawer update
            messagingTemplate.convertAndSend("/topic/alerts/updates", push);

            // Also deliver to order-specific topic if related to an order
            if (push.relatedOrderId() != null && !push.relatedOrderId().isBlank()) {
                messagingTemplate.convertAndSend("/topic/orders/" + push.relatedOrderId() + "/updates", push);
            }
        } catch (Exception ignored) {
            // Malformed fan-out payloads are dropped rather than crashing the shared Redis listener thread.
        }
    }
}
