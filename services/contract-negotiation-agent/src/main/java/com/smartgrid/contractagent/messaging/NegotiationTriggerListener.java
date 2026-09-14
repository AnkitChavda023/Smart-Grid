package com.smartgrid.contractagent.messaging;

import com.smartgrid.commons.avro.sla.ContractExpiring;
import com.smartgrid.commons.avro.sla.SLARiskReportGenerated;
import com.smartgrid.contractagent.service.ContractNegotiationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NegotiationTriggerListener {

    private final ContractNegotiationService negotiationService;

    public NegotiationTriggerListener(ContractNegotiationService negotiationService) {
        this.negotiationService = negotiationService;
    }

    @KafkaListener(topics = "sla-events", groupId = "${smartgrid.kafka.group-id}-negotiation")
    public void onSlaEvent(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof SLARiskReportGenerated riskReport) {
            negotiationService.negotiate(riskReport.getVendorId());
        } else if (record.value() instanceof ContractExpiring expiring) {
            negotiationService.negotiate(expiring.getVendorId());
        }
    }
}
