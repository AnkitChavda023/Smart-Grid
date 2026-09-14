package com.smartgrid.inventoryservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ReserveRequest(
        @NotBlank String orderId,
        @NotBlank String skuId,
        @NotBlank String warehouseId,
        @Positive long quantity
) {
}
