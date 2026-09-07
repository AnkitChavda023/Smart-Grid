package com.smartgrid.notificationservice.dto;

import com.smartgrid.notificationservice.domain.NotificationChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PreferenceRequest(
        @NotBlank String userId,
        @NotNull NotificationChannelType channel
) {
}
