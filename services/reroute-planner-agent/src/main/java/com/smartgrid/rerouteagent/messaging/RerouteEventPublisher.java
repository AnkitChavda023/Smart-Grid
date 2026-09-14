package com.smartgrid.rerouteagent.messaging;

import com.smartgrid.commons.avro.reroute.EscalationRequired;
import com.smartgrid.commons.avro.reroute.RerouteDecision;
import com.smartgrid.commons.web.CorrelationContext;
import com.smartgrid.rerouteagent.domain.Reroute;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RerouteEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RerouteEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishDecision(Reroute reroute) {
        RerouteDecision event = RerouteDecision.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setRerouteId(reroute.getId().toString())
                .setOrderId(reroute.getOrderId())
                .setDisruptionId(reroute.getDisruptionId())
                .setSelectedVendorId(reroute.getSelectedVendorId())
                .setQuoteId(reroute.getQuoteId())
                .setConfidence(reroute.getConfidence())
                .setAgentTrace(reroute.getAgentTraceJson())
                .build();
        kafkaTemplate.send("reroute-decisions", reroute.getOrderId(), event);
    }

    public void publishEscalation(Reroute reroute, String reason) {
        EscalationRequired event = EscalationRequired.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setOrderId(reroute.getOrderId())
                .setDisruptionId(reroute.getDisruptionId())
                .setReason(reason)
                .setConfidence(reroute.getConfidence())
                .build();
        kafkaTemplate.send("reroute-decisions", reroute.getOrderId(), event);
    }
}
