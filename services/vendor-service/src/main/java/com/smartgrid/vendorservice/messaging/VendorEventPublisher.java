package com.smartgrid.vendorservice.messaging;

import com.smartgrid.commons.avro.vendor.VendorConfirmed;
import com.smartgrid.commons.avro.vendor.VendorScoreUpdated;
import com.smartgrid.commons.avro.vendor.VendorSuspended;
import com.smartgrid.commons.web.CorrelationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class VendorEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public VendorEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishScoreUpdated(String vendorId, String skuId, double previousScore, double newScore, String reason) {
        VendorScoreUpdated event = VendorScoreUpdated.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setVendorId(vendorId)
                .setSkuId(skuId)
                .setPreviousScore(previousScore)
                .setNewScore(newScore)
                .setReason(reason)
                .build();
        kafkaTemplate.send("vendor-events", vendorId, event);
    }

    public void publishVendorConfirmed(String orderId, String vendorId, String skuId, int quantity) {
        VendorConfirmed event = VendorConfirmed.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setOrderId(orderId)
                .setVendorId(vendorId)
                .setSkuId(skuId)
                .setQuantity(quantity)
                .build();
        kafkaTemplate.send("vendor-events", vendorId, event);
    }

    public void publishSuspended(String vendorId, String reason) {
        VendorSuspended event = VendorSuspended.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setVendorId(vendorId)
                .setReason(reason)
                .build();
        kafkaTemplate.send("vendor-events", vendorId, event);
    }
}
