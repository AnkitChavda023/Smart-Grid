package com.smartgrid.slabreachagent.messaging;

import com.smartgrid.commons.avro.sla.SLARiskReportGenerated;
import com.smartgrid.commons.web.CorrelationContext;
import com.smartgrid.slabreachagent.domain.BreachRiskAssessment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SlaRiskReportEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SlaRiskReportEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(BreachRiskAssessment assessment) {
        SLARiskReportGenerated event = SLARiskReportGenerated.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setVendorId(assessment.getVendorId())
                .setBreachProbability(assessment.getBreachProbability())
                .setConfidence(assessment.getConfidence())
                .setSummary(assessment.getSummary())
                .build();
        kafkaTemplate.send("sla-events", assessment.getVendorId(), event);
    }
}
