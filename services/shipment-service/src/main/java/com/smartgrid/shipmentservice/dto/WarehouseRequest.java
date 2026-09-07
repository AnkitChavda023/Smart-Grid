package com.smartgrid.shipmentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WarehouseRequest(
        @NotBlank String name,
        @NotNull Double latitude,
        @NotNull Double longitude
) {
}
