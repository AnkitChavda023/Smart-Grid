package com.smartgrid.inventoryservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_snapshot", uniqueConstraints = @UniqueConstraint(columnNames = {"sku_id", "warehouse_id"}))
public class InventorySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sku_id", nullable = false)
    private String skuId;

    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId;

    @Column(name = "available_quantity", nullable = false)
    private long availableQuantity;

    @Column(name = "last_event_id", nullable = false)
    private long lastEventId;

    /** columnDefinition carries an explicit DEFAULT so Hibernate's ALTER TABLE succeeds against a table that already has rows. */
    @Column(name = "safety_stock_level", nullable = false, columnDefinition = "bigint not null default 0")
    private long safetyStockLevel = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected InventorySnapshot() {
    }

    public InventorySnapshot(String skuId, String warehouseId, long availableQuantity, long lastEventId) {
        this.skuId = skuId;
        this.warehouseId = warehouseId;
        this.availableQuantity = availableQuantity;
        this.lastEventId = lastEventId;
    }

    public void apply(long quantityDelta, long eventId) {
        this.availableQuantity += quantityDelta;
        this.lastEventId = eventId;
        this.updatedAt = Instant.now();
    }

    public void replaceWith(long recomputedQuantity, long lastEventId) {
        this.availableQuantity = recomputedQuantity;
        this.lastEventId = lastEventId;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getSkuId() {
        return skuId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public long getAvailableQuantity() {
        return availableQuantity;
    }

    public long getLastEventId() {
        return lastEventId;
    }

    public long getSafetyStockLevel() {
        return safetyStockLevel;
    }

    public void setSafetyStockLevel(long safetyStockLevel) {
        this.safetyStockLevel = safetyStockLevel;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
