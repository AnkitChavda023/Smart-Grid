package com.smartgrid.notificationservice.dto;

import com.smartgrid.notificationservice.domain.Notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id, String userId, String title, String body, String relatedOrderId, boolean read, Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getUserId(), notification.getTitle(),
                notification.getBody(), notification.getRelatedOrderId(), notification.isRead(), notification.getCreatedAt());
    }
}
