package com.smartgrid.shipmentservice.messaging;

import com.smartgrid.commons.avro.order.OrderFulfilled;
import com.smartgrid.commons.avro.reroute.RerouteDecision;
import com.smartgrid.shipmentservice.domain.Shipment;
import com.smartgrid.shipmentservice.repository.ShipmentRepository;
import com.smartgrid.shipmentservice.repository.WarehouseRepository;
import com.smartgrid.shipmentservice.service.ShipmentService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.locationtech.jts.geom.Point;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ShipmentOrderListener {

    private final ShipmentService shipmentService;
    private final ShipmentRepository shipmentRepository;
    private final WarehouseRepository warehouseRepository;

    public ShipmentOrderListener(ShipmentService shipmentService, ShipmentRepository shipmentRepository, WarehouseRepository warehouseRepository) {
        this.shipmentService = shipmentService;
        this.shipmentRepository = shipmentRepository;
        this.warehouseRepository = warehouseRepository;
    }

    @KafkaListener(topics = "order-events", groupId = "${smartgrid.kafka.group-id}-orders")
    public void onOrderEvent(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof OrderFulfilled fulfilled) {
            Point origin = warehouseRepository.findAll().stream()
                    .findFirst()
                    .map(w -> w.getLocation())
                    .orElse(null);
            String destination = fulfilled.getDestinationRegion() != null ? fulfilled.getDestinationRegion() : "unknown";
            shipmentService.createShipment(fulfilled.getOrderId(), fulfilled.getVendorId(), destination, origin);
        }
    }

    @KafkaListener(topics = "reroute-decisions", groupId = "${smartgrid.kafka.group-id}-reroute")
    @Transactional
    public void onRerouteDecision(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof RerouteDecision decision) {
            shipmentRepository.findByOrderId(decision.getOrderId()).ifPresent(shipment -> {
                shipment.setOriginWarehouseId(decision.getSelectedVendorId());
                shipmentRepository.save(shipment);
            });
        }
    }
}
