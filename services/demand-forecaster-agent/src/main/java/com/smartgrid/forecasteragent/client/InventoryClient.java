package com.smartgrid.forecasteragent.client;

import com.smartgrid.forecasteragent.config.DemandForecasterProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;

@Component
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(DemandForecasterProperties properties) {
        this.restClient = RestClient.create(properties.inventoryService().baseUrl());
    }

    public List<String> allSkuIds() {
        String[] skus = restClient.get().uri("/inventory/skus").retrieve().body(String[].class);
        return skus == null ? List.of() : List.of(skus);
    }

    public List<DemandEvent> demandEventsFor(String skuId) {
        DemandEvent[] events = restClient.get().uri("/inventory/{sku}/demand-events", skuId).retrieve().body(DemandEvent[].class);
        return events == null ? List.of() : List.of(events);
    }

    public long totalAvailable(String skuId) {
        Availability availability = restClient.get().uri("/inventory/{sku}/available", skuId).retrieve().body(Availability.class);
        return availability == null ? 0 : availability.available();
    }

    public List<String> warehousesFor(String skuId) {
        WarehouseSnapshot[] snapshots = restClient.get().uri("/inventory/{sku}/warehouses", skuId).retrieve().body(WarehouseSnapshot[].class);
        return snapshots == null ? List.of() : List.of(snapshots).stream().map(WarehouseSnapshot::warehouseId).toList();
    }

    public record DemandEvent(String skuId, long quantity, Instant occurredAt) {
    }

    private record Availability(String skuId, long available) {
    }

    private record WarehouseSnapshot(String skuId, String warehouseId, long availableQuantity, long safetyStockLevel) {
    }
}
