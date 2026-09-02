package com.smartgrid.orderservice.messaging;

import com.smartgrid.commons.kafka.OutboxEventSerializer;
import com.smartgrid.orderservice.domain.OutboxEvent;
import com.smartgrid.orderservice.repository.OutboxEventRepository;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {

    private static final String AGGREGATE_TYPE = "order";
    private static final String TARGET_TOPIC = "order-events";

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventSerializer serializer;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, OutboxEventSerializer serializer) {
        this.outboxEventRepository = outboxEventRepository;
        this.serializer = serializer;
    }

    public void publish(String orderId, String eventType, SpecificRecordBase event) {
        byte[] payload = serializer.serialize(TARGET_TOPIC, event);
        outboxEventRepository.save(new OutboxEvent(AGGREGATE_TYPE, orderId, eventType, payload));
    }
}
