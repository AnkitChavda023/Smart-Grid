package com.smartgrid.shipmentservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "origin_warehouse_id", nullable = false)
    private String originWarehouseId;

    @Column(nullable = false)
    private String destination;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status = ShipmentStatus.DISPATCHED;

    @Column(name = "eta_minutes")
    private Double etaMinutes;

    @Column(name = "checkpoint_count", nullable = false)
    private int checkpointCount = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    protected Shipment() {
    }

    public Shipment(String orderId, String originWarehouseId, String destination) {
        this.orderId = orderId;
        this.originWarehouseId = originWarehouseId;
        this.destination = destination;
    }

    public UUID getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getOriginWarehouseId() {
        return originWarehouseId;
    }

    public void setOriginWarehouseId(String originWarehouseId) {
        this.originWarehouseId = originWarehouseId;
    }

    public String getDestination() {
        return destination;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public Double getEtaMinutes() {
        return etaMinutes;
    }

    public void setEtaMinutes(Double etaMinutes) {
        this.etaMinutes = etaMinutes;
    }

    public int getCheckpointCount() {
        return checkpointCount;
    }

    public void incrementCheckpointCount() {
        this.checkpointCount++;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
}
