package com.smartgrid.slabreachagent.client;

import com.smartgrid.slabreachagent.config.SlaBreachAnalystProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Component
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(SlaBreachAnalystProperties properties) {
        this.restClient = RestClient.create(properties.inventoryService().baseUrl());
    }

    /** The warehouse holding the least stock for this SKU is the one most exposed to a predicted breach. */
    public Optional<WarehouseSnapshot> mostAtRiskWarehouse(String skuId) {
        WarehouseSnapshot[] snapshots = restClient.get()
                .uri("/inventory/{sku}/warehouses", skuId)
                .retrieve()
                .body(WarehouseSnapshot[].class);
        return snapshots == null ? Optional.empty() : List.of(snapshots).stream()
                .min((a, b) -> Long.compare(a.availableQuantity(), b.availableQuantity()));
    }

    public record WarehouseSnapshot(String skuId, String warehouseId, long availableQuantity, long safetyStockLevel) {
    }
}
