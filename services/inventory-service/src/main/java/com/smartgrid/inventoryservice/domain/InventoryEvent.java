package com.smartgrid.inventoryservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "inventory_events")
public class InventoryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku_id", nullable = false)
    private String skuId;

    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private InventoryEventType eventType;

    @Column(name = "quantity_delta", nullable = false)
    private long quantityDelta;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected InventoryEvent() {
    }

    public InventoryEvent(String skuId, String warehouseId, InventoryEventType eventType, long quantityDelta, String orderId) {
        this.skuId = skuId;
        this.warehouseId = warehouseId;
        this.eventType = eventType;
        this.quantityDelta = quantityDelta;
        this.orderId = orderId;
    }

    public Long getId() {
        return id;
    }

    public String getSkuId() {
        return skuId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public InventoryEventType getEventType() {
        return eventType;
    }

    public long getQuantityDelta() {
        return quantityDelta;
    }

    public String getOrderId() {
        return orderId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
