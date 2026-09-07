package com.smartgrid.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;

public record AdhocNotificationRequest(@NotBlank String title, @NotBlank String body, String relatedEntityId) {
}
