package com.smartgrid.shipmentservice.web;

import com.smartgrid.commons.exception.ResourceNotFoundException;
import com.smartgrid.shipmentservice.dto.CheckpointRequest;
import com.smartgrid.shipmentservice.dto.ShipmentResponse;
import com.smartgrid.shipmentservice.repository.ShipmentRepository;
import com.smartgrid.shipmentservice.service.ShipmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final ShipmentRepository shipmentRepository;

    public ShipmentController(ShipmentService shipmentService, ShipmentRepository shipmentRepository) {
        this.shipmentService = shipmentService;
        this.shipmentRepository = shipmentRepository;
    }

    @GetMapping("/{id}")
    public ShipmentResponse getShipment(@PathVariable UUID id) {
        return ShipmentResponse.from(shipmentService.getShipment(id));
    }

    @GetMapping
    public List<ShipmentResponse> listShipments(@RequestParam(required = false) String orderId) {
        if (orderId != null && !orderId.isBlank()) {
            return shipmentRepository.findByOrderId(orderId)
                    .map(ShipmentResponse::from)
                    .map(List::of)
                    .orElse(List.of());
        }
        return shipmentRepository.findAll().stream()
                .map(ShipmentResponse::from)
                .toList();
    }

    @PostMapping("/{id}/checkpoint")
    public ShipmentResponse recordCheckpoint(@PathVariable UUID id, @Valid @RequestBody CheckpointRequest request) {
        return ShipmentResponse.from(shipmentService.recordCheckpoint(id, request.latitude(), request.longitude()));
    }

    @GetMapping("/{id}/eta")
    public ResponseEntity<Double> getEta(@PathVariable UUID id) {
        return ResponseEntity.ok(shipmentService.getShipment(id).getEtaMinutes());
    }

    @PostMapping("/{id}/deliver")
    public ResponseEntity<Void> markDelivered(@PathVariable UUID id) {
        shipmentService.markDelivered(id);
        return ResponseEntity.noContent().build();
    }
}
