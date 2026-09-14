package com.smartgrid.inventoryservice.messaging;

import com.smartgrid.commons.avro.inventory.ReservationConfirmed;
import com.smartgrid.commons.avro.inventory.StockDepleted;
import com.smartgrid.commons.avro.inventory.StockReplenished;
import com.smartgrid.commons.web.CorrelationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InventoryEventPublisher {

    private static final String TOPIC = "inventory-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishReservationConfirmed(String orderId, String skuId, long quantity, String warehouseId) {
        ReservationConfirmed event = ReservationConfirmed.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setOrderId(orderId)
                .setSkuId(skuId)
                .setQuantity((int) quantity)
                .setWarehouseId(warehouseId)
                .build();
        kafkaTemplate.send(TOPIC, orderId, event);
    }

    public void publishStockDepleted(String skuId, String warehouseId) {
        StockDepleted event = StockDepleted.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setSkuId(skuId)
                .setWarehouseId(warehouseId)
                .build();
        kafkaTemplate.send(TOPIC, skuId, event);
    }

    public void publishStockReplenished(String skuId, String warehouseId, long quantity) {
        StockReplenished event = StockReplenished.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setSkuId(skuId)
                .setWarehouseId(warehouseId)
                .setQuantity((int) quantity)
                .build();
        kafkaTemplate.send(TOPIC, skuId, event);
    }
}
