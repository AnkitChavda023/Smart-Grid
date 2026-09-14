package com.smartgrid.shipmentservice.repository;

import com.smartgrid.shipmentservice.domain.ShipmentCheckpoint;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShipmentCheckpointRepository extends JpaRepository<ShipmentCheckpoint, UUID> {

    List<ShipmentCheckpoint> findByShipmentIdOrderByRecordedAtDesc(UUID shipmentId, Limit limit);
}
