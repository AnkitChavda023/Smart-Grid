package com.smartgrid.contractservice.messaging;

import com.smartgrid.commons.avro.contract.ContractIndexed;
import com.smartgrid.commons.web.CorrelationContext;
import com.smartgrid.contractservice.domain.Contract;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ContractSyncPublisher {

    private static final String TOPIC = "contract-sync-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ContractSyncPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishIndexed(Contract contract) {
        ContractIndexed event = ContractIndexed.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setContractId(contract.getId().toString())
                .setVendorId(contract.getVendorId())
                .setTerms(contract.getTerms())
                .setStartDate(contract.getStartDate().toString())
                .setEndDate(contract.getEndDate().toString())
                .setActive(contract.isActive())
                .build();
        kafkaTemplate.send(TOPIC, contract.getId().toString(), event);
    }
}
