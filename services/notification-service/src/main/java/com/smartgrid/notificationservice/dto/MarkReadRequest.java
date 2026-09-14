package com.smartgrid.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MarkReadRequest(
        @NotBlank String userId,
        @NotNull UUID notificationId
) {
}
