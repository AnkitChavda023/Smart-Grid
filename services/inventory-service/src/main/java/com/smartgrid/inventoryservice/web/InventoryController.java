package com.smartgrid.inventoryservice.web;

import com.smartgrid.inventoryservice.dto.AvailabilityResponse;
import com.smartgrid.inventoryservice.dto.DemandEventResponse;
import com.smartgrid.inventoryservice.dto.ReleaseRequest;
import com.smartgrid.inventoryservice.dto.ReplenishRequest;
import com.smartgrid.inventoryservice.dto.ReserveRequest;
import com.smartgrid.inventoryservice.dto.SafetyStockRequest;
import com.smartgrid.inventoryservice.dto.WarehouseSnapshotResponse;
import com.smartgrid.inventoryservice.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{sku}/available")
    public AvailabilityResponse available(@PathVariable("sku") String skuId) {
        return new AvailabilityResponse(skuId, inventoryService.totalAvailable(skuId));
    }

    @GetMapping("/{sku}/warehouses")
    public List<WarehouseSnapshotResponse> warehousesForSku(@PathVariable("sku") String skuId) {
        return inventoryService.snapshotsForSku(skuId).stream()
                .map(WarehouseSnapshotResponse::from)
                .toList();
    }

    @GetMapping("/skus")
    public List<String> allSkuIds() {
        return inventoryService.allSkuIds();
    }

    @GetMapping("/{sku}/demand-events")
    public List<DemandEventResponse> demandEvents(@PathVariable("sku") String skuId) {
        return inventoryService.demandEventsForSku(skuId).stream()
                .map(DemandEventResponse::from)
                .toList();
    }

    @PostMapping("/reserve")
    public ResponseEntity<Void> reserve(@Valid @RequestBody ReserveRequest request) {
        inventoryService.reserve(request.orderId(), request.skuId(), request.warehouseId(), request.quantity());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/release")
    public ResponseEntity<Void> release(@Valid @RequestBody ReleaseRequest request) {
        inventoryService.release(request.orderId(), request.skuId(), request.warehouseId(), request.quantity());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/replenish")
    public ResponseEntity<Void> replenish(@Valid @RequestBody ReplenishRequest request) {
        inventoryService.replenish(request.skuId(), request.warehouseId(), request.quantity());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{sku}/{warehouseId}/safety-stock")
    public ResponseEntity<Void> updateSafetyStock(
            @PathVariable("sku") String skuId, @PathVariable String warehouseId, @Valid @RequestBody SafetyStockRequest request) {
        inventoryService.updateSafetyStockLevel(skuId, warehouseId, request.safetyStockLevel());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/warehouse/{id}")
    public List<WarehouseSnapshotResponse> warehouseInventory(@PathVariable("id") String warehouseId) {
        return inventoryService.warehouseInventory(warehouseId).stream()
                .map(WarehouseSnapshotResponse::from)
                .toList();
    }
}
