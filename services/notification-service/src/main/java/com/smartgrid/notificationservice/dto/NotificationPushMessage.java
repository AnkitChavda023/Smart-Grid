package com.smartgrid.notificationservice.dto;

public record NotificationPushMessage(
        String notificationId,
        String title,
        String body,
        String relatedOrderId
) {
}
