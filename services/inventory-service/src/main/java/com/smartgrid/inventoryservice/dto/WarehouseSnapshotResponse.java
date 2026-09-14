package com.smartgrid.inventoryservice.dto;

import com.smartgrid.inventoryservice.domain.InventorySnapshot;

public record WarehouseSnapshotResponse(String skuId, String warehouseId, long availableQuantity, long safetyStockLevel) {

    public static WarehouseSnapshotResponse from(InventorySnapshot snapshot) {
        return new WarehouseSnapshotResponse(snapshot.getSkuId(), snapshot.getWarehouseId(),
                snapshot.getAvailableQuantity(), snapshot.getSafetyStockLevel());
    }
}
