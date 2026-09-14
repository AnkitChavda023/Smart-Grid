package com.smartgrid.shipmentservice.dto;

import jakarta.validation.constraints.NotNull;

public record CheckpointRequest(
        @NotNull Double latitude,
        @NotNull Double longitude
) {
}
