package com.smartgrid.shipmentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateShipmentRequest(
        @NotBlank String orderId,
        @NotBlank String originWarehouseId,
        @NotBlank String destination,
        @NotNull Double originLatitude,
        @NotNull Double originLongitude
) {
}
