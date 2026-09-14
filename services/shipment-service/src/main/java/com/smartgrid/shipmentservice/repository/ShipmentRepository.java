package com.smartgrid.shipmentservice.repository;

import com.smartgrid.shipmentservice.domain.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    Optional<Shipment> findByOrderId(String orderId);
}
