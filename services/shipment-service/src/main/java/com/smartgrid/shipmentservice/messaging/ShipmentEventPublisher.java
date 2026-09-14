package com.smartgrid.shipmentservice.messaging;

import com.smartgrid.commons.avro.shipment.ShipmentDelayed;
import com.smartgrid.commons.avro.shipment.ShipmentDelivered;
import com.smartgrid.commons.avro.shipment.ShipmentDispatched;
import com.smartgrid.commons.web.CorrelationContext;
import com.smartgrid.shipmentservice.domain.Shipment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ShipmentEventPublisher {

    private static final String TOPIC = "shipment-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ShipmentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishDispatched(Shipment shipment) {
        ShipmentDispatched event = ShipmentDispatched.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setShipmentId(shipment.getId().toString())
                .setOrderId(shipment.getOrderId())
                .setOriginWarehouseId(shipment.getOriginWarehouseId())
                .setDestination(shipment.getDestination())
                .build();
        kafkaTemplate.send(TOPIC, shipment.getId().toString(), event);
    }

    public void publishDelayed(Shipment shipment, int delayMinutes, String reason) {
        ShipmentDelayed event = ShipmentDelayed.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setShipmentId(shipment.getId().toString())
                .setOrderId(shipment.getOrderId())
                .setDelayMinutes(delayMinutes)
                .setReason(reason)
                .build();
        kafkaTemplate.send(TOPIC, shipment.getId().toString(), event);
    }

    public void publishDelivered(Shipment shipment) {
        ShipmentDelivered event = ShipmentDelivered.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setShipmentId(shipment.getId().toString())
                .setOrderId(shipment.getOrderId())
                .build();
        kafkaTemplate.send(TOPIC, shipment.getId().toString(), event);
    }
}
