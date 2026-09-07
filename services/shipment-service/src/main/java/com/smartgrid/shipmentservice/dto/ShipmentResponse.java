package com.smartgrid.shipmentservice.dto;

import com.smartgrid.shipmentservice.domain.Shipment;
import com.smartgrid.shipmentservice.domain.ShipmentStatus;

import java.time.Instant;
import java.util.UUID;

public record ShipmentResponse(
        UUID id,
        String orderId,
        String originWarehouseId,
        String destination,
        ShipmentStatus status,
        Double etaMinutes,
        int checkpointCount,
        Instant createdAt
) {
    public static ShipmentResponse from(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getId(), shipment.getOrderId(), shipment.getOriginWarehouseId(), shipment.getDestination(),
                shipment.getStatus(), shipment.getEtaMinutes(), shipment.getCheckpointCount(), shipment.getCreatedAt()
        );
    }
}
