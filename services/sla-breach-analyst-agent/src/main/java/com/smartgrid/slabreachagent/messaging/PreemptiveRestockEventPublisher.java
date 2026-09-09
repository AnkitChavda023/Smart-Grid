package com.smartgrid.slabreachagent.messaging;

import com.smartgrid.commons.avro.inventory.PreemptiveRestockTriggered;
import com.smartgrid.commons.web.CorrelationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PreemptiveRestockEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PreemptiveRestockEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String skuId, String warehouseId, long quantity, String reason) {
        PreemptiveRestockTriggered event = PreemptiveRestockTriggered.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setSkuId(skuId)
                .setWarehouseId(warehouseId)
                .setQuantity(quantity)
                .setReason(reason)
                .build();
        kafkaTemplate.send("inventory-events", skuId, event);
    }
}
