package com.smartgrid.forecasteragent.messaging;

import com.smartgrid.commons.avro.demand.SafetyStockUpdateRecommended;
import com.smartgrid.commons.web.CorrelationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SafetyStockUpdateEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SafetyStockUpdateEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String skuId, String warehouseId, long recommendedSafetyStockLevel, String reason) {
        SafetyStockUpdateRecommended event = SafetyStockUpdateRecommended.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setSkuId(skuId)
                .setWarehouseId(warehouseId)
                .setRecommendedSafetyStockLevel(recommendedSafetyStockLevel)
                .setReason(reason)
                .build();
        kafkaTemplate.send("inventory-events", skuId, event);
    }
}
