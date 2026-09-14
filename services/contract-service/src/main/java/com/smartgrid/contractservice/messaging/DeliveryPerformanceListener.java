package com.smartgrid.contractservice.messaging;

import com.smartgrid.commons.avro.order.OrderFulfilled;
import com.smartgrid.commons.avro.shipment.ShipmentDelivered;
import com.smartgrid.contractservice.domain.VendorDeliveryRecord;
import com.smartgrid.contractservice.repository.VendorDeliveryRecordRepository;
import com.smartgrid.contractservice.service.SlaBreachDetectionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Component
public class DeliveryPerformanceListener {

    private final VendorDeliveryRecordRepository deliveryRecordRepository;
    private final SlaBreachDetectionService slaBreachDetectionService;

    public DeliveryPerformanceListener(
            VendorDeliveryRecordRepository deliveryRecordRepository,
            SlaBreachDetectionService slaBreachDetectionService
    ) {
        this.deliveryRecordRepository = deliveryRecordRepository;
        this.slaBreachDetectionService = slaBreachDetectionService;
    }

    @KafkaListener(topics = "order-events", groupId = "${smartgrid.kafka.group-id}-orders")
    @Transactional
    public void onOrderEvent(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof OrderFulfilled fulfilled) {
            deliveryRecordRepository.save(
                    new VendorDeliveryRecord(fulfilled.getOrderId(), fulfilled.getVendorId(), Instant.now()));
        }
    }

    @KafkaListener(topics = "shipment-events", groupId = "${smartgrid.kafka.group-id}-shipments")
    @Transactional
    public void onShipmentEvent(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof ShipmentDelivered delivered) {
            deliveryRecordRepository.findById(delivered.getOrderId()).ifPresent(deliveryRecord -> {
                deliveryRecord.setDelivered(true);
                deliveryRecordRepository.save(deliveryRecord);

                double actualLeadTimeDays = Duration.between(deliveryRecord.getFulfilledAt(), Instant.now()).toMillis() / 86_400_000.0;
                slaBreachDetectionService.evaluateLeadTime(
                        deliveryRecord.getVendorId(), actualLeadTimeDays,
                        "Order " + deliveryRecord.getOrderId() + " delivered after " + actualLeadTimeDays + " day(s)");
            });
        }
    }
}
