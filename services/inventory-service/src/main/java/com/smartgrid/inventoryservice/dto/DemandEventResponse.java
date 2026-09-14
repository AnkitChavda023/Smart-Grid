package com.smartgrid.inventoryservice.dto;

import com.smartgrid.inventoryservice.domain.InventoryEvent;

import java.time.Instant;

public record DemandEventResponse(String skuId, long quantity, Instant occurredAt) {

    public static DemandEventResponse from(InventoryEvent event) {
        return new DemandEventResponse(event.getSkuId(), -event.getQuantityDelta(), event.getCreatedAt());
    }
}
