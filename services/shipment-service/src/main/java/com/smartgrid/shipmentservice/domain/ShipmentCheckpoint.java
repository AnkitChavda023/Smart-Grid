package com.smartgrid.shipmentservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipment_checkpoints")
public class ShipmentCheckpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point location;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt = Instant.now();

    protected ShipmentCheckpoint() {
    }

    public ShipmentCheckpoint(Shipment shipment, Point location) {
        this.shipment = shipment;
        this.location = location;
    }

    public ShipmentCheckpoint(Shipment shipment, Point location, Instant recordedAt) {
        this.shipment = shipment;
        this.location = location;
        this.recordedAt = recordedAt;
    }

    public UUID getId() {
        return id;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public Point getLocation() {
        return location;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
