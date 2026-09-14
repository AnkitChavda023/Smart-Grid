package com.smartgrid.shipmentservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Point;

import java.util.UUID;

@Entity
@Table(name = "shipment_routes")
public class ShipmentRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "shipment_id", nullable = false, unique = true)
    private Shipment shipment;

    @Column(name = "origin_location", nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point originLocation;

    @Column(name = "destination_location", columnDefinition = "geometry(Point,4326)")
    private Point destinationLocation;

    protected ShipmentRoute() {
    }

    public ShipmentRoute(Shipment shipment, Point originLocation, Point destinationLocation) {
        this.shipment = shipment;
        this.originLocation = originLocation;
        this.destinationLocation = destinationLocation;
    }

    public UUID getId() {
        return id;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public Point getOriginLocation() {
        return originLocation;
    }

    public Point getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(Point destinationLocation) {
        this.destinationLocation = destinationLocation;
    }
}
