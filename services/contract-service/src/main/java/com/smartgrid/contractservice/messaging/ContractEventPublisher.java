package com.smartgrid.contractservice.messaging;

import com.smartgrid.commons.avro.sla.ContractExpiring;
import com.smartgrid.commons.avro.sla.PenaltyComputed;
import com.smartgrid.commons.avro.sla.SLABreached;
import com.smartgrid.commons.web.CorrelationContext;
import com.smartgrid.contractservice.domain.Contract;
import com.smartgrid.contractservice.domain.SlaBreach;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ContractEventPublisher {

    private static final String TOPIC = "sla-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ContractEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishSlaBreached(SlaBreach breach) {
        SLABreached event = SLABreached.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setBreachId(breach.getId().toString())
                .setVendorId(breach.getVendorId())
                .setContractId(breach.getContractId().toString())
                .setSeverity(breach.getSeverity())
                .setPenaltyAmount(breach.getPenaltyAmount())
                .build();
        kafkaTemplate.send(TOPIC, breach.getVendorId(), event);
    }

    public void publishContractExpiring(Contract contract, int daysRemaining) {
        ContractExpiring event = ContractExpiring.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setContractId(contract.getId().toString())
                .setVendorId(contract.getVendorId())
                .setDaysRemaining(daysRemaining)
                .build();
        kafkaTemplate.send(TOPIC, contract.getVendorId(), event);
    }

    public void publishPenaltyComputed(UUID contractId, String vendorId, double amount) {
        PenaltyComputed event = PenaltyComputed.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setContractId(contractId.toString())
                .setVendorId(vendorId)
                .setAmount(amount)
                .build();
        kafkaTemplate.send(TOPIC, vendorId, event);
    }
}
