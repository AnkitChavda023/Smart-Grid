package com.smartgrid.shipmentservice.service;

import com.smartgrid.commons.exception.ResourceNotFoundException;
import com.smartgrid.shipmentservice.domain.Shipment;
import com.smartgrid.shipmentservice.domain.ShipmentCheckpoint;
import com.smartgrid.shipmentservice.domain.ShipmentRoute;
import com.smartgrid.shipmentservice.domain.ShipmentStatus;
import com.smartgrid.shipmentservice.dto.LiveLocationMessage;
import com.smartgrid.shipmentservice.messaging.ShipmentEventPublisher;
import com.smartgrid.shipmentservice.repository.ShipmentCheckpointRepository;
import com.smartgrid.shipmentservice.repository.ShipmentRepository;
import com.smartgrid.shipmentservice.repository.ShipmentRouteRepository;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Limit;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ShipmentService {

    private static final int EXPECTED_TOTAL_CHECKPOINTS = 10;
    private static final int WMA_WINDOW = 5;

    private final ShipmentRepository shipmentRepository;
    private final ShipmentCheckpointRepository checkpointRepository;
    private final ShipmentRouteRepository routeRepository;
    private final EtaCalculationService etaCalculationService;
    private final ShipmentEventPublisher publisher;
    private final SimpMessagingTemplate messagingTemplate;

    public ShipmentService(
            ShipmentRepository shipmentRepository,
            ShipmentCheckpointRepository checkpointRepository,
            ShipmentRouteRepository routeRepository,
            EtaCalculationService etaCalculationService,
            ShipmentEventPublisher publisher,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.shipmentRepository = shipmentRepository;
        this.checkpointRepository = checkpointRepository;
        this.routeRepository = routeRepository;
        this.etaCalculationService = etaCalculationService;
        this.publisher = publisher;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public Shipment createShipment(String orderId, String originWarehouseId, String destination, Point originPoint) {
        Shipment shipment = shipmentRepository.save(new Shipment(orderId, originWarehouseId, destination));
        routeRepository.save(new ShipmentRoute(shipment, originPoint, null));
        publisher.publishDispatched(shipment);
        return shipment;
    }

    @Transactional(readOnly = true)
    public Shipment getShipment(UUID id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", id.toString()));
    }

    @Transactional
    public Shipment recordCheckpoint(UUID shipmentId, double latitude, double longitude) {
        Shipment shipment = getShipment(shipmentId);
        Point point = GeoUtil.point(latitude, longitude);
        Instant now = Instant.now();

        List<ShipmentCheckpoint> priorDescending = checkpointRepository.findByShipmentIdOrderByRecordedAtDesc(shipmentId, Limit.of(WMA_WINDOW));
        checkpointRepository.save(new ShipmentCheckpoint(shipment, point, now));
        shipment.incrementCheckpointCount();

        List<Instant> chronological = new ArrayList<>();
        for (int i = priorDescending.size() - 1; i >= 0; i--) {
            chronological.add(priorDescending.get(i).getRecordedAt());
        }
        chronological.add(now);

        int remaining = Math.max(0, EXPECTED_TOTAL_CHECKPOINTS - shipment.getCheckpointCount());
        EtaCalculationService.EtaResult result = etaCalculationService.computeEta(chronological, remaining);

        if (result.etaMinutes() != null) {
            shipment.setEtaMinutes(result.etaMinutes());
        }

        if (shipment.getStatus() != ShipmentStatus.DELIVERED) {
            if (result.delayed()) {
                shipment.setStatus(ShipmentStatus.DELAYED);
                int delayMinutes = (int) Math.round(result.latestGapMinutes() - result.baselineAvgMinutes());
                publisher.publishDelayed(shipment, delayMinutes, "Checkpoint gap exceeded expected pace");
            } else if (shipment.getStatus() == ShipmentStatus.DISPATCHED) {
                shipment.setStatus(ShipmentStatus.IN_TRANSIT);
            }
        }

        Shipment saved = shipmentRepository.save(shipment);

        messagingTemplate.convertAndSend(
                "/topic/shipments/" + shipmentId + "/live",
                new LiveLocationMessage(latitude, longitude, saved.getEtaMinutes(), saved.getStatus().name()));

        return saved;
    }

    @Transactional
    public void markDelivered(UUID shipmentId) {
        Shipment shipment = getShipment(shipmentId);
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveredAt(Instant.now());
        shipmentRepository.save(shipment);
        publisher.publishDelivered(shipment);
    }
}
