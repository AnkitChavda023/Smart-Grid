package com.smartgrid.contractagent.messaging;

import com.smartgrid.commons.avro.contract.ContractDraftGenerated;
import com.smartgrid.commons.web.CorrelationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Publishes draft id + summary only, never the drafted contract terms themselves — per the module spec's confidentiality note. */
@Component
public class ContractDraftEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ContractDraftEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String draftId, String vendorId, String summary) {
        ContractDraftGenerated event = ContractDraftGenerated.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setDraftId(draftId)
                .setVendorId(vendorId)
                .setSummary(summary)
                .build();
        kafkaTemplate.send("contract-sync-events", vendorId, event);
    }
}
