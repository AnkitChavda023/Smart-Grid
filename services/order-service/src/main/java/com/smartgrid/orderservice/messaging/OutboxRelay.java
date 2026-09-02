package com.smartgrid.orderservice.messaging;

import com.smartgrid.orderservice.domain.OutboxEvent;
import com.smartgrid.orderservice.repository.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxRelay {

    private static final String TARGET_TOPIC = "order-events";
    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public OutboxRelay(OutboxEventRepository outboxEventRepository,
                        @Qualifier("outboxRelayKafkaTemplate") KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 200)
    @Transactional
    public void relayPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc(Limit.of(BATCH_SIZE));
        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(TARGET_TOPIC, event.getAggregateId(), event.getPayload()).get(10, TimeUnit.SECONDS);
                event.markPublished();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to relay outbox event " + event.getId(), e);
            }
        }
    }
}
