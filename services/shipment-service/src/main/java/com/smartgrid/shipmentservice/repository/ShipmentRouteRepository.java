package com.smartgrid.shipmentservice.repository;

import com.smartgrid.shipmentservice.domain.ShipmentRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShipmentRouteRepository extends JpaRepository<ShipmentRoute, UUID> {

    Optional<ShipmentRoute> findByShipmentId(UUID shipmentId);
}
