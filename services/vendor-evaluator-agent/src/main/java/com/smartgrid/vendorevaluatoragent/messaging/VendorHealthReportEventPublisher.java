package com.smartgrid.vendorevaluatoragent.messaging;

import com.smartgrid.commons.avro.vendor.VendorHealthReportGenerated;
import com.smartgrid.commons.web.CorrelationContext;
import com.smartgrid.vendorevaluatoragent.domain.VendorEvaluation;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class VendorHealthReportEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public VendorHealthReportEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(VendorEvaluation evaluation) {
        VendorHealthReportGenerated event = VendorHealthReportGenerated.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setVendorId(evaluation.getVendorId())
                .setTrendDirection(evaluation.getTrendDirection())
                .setConfidence(evaluation.getConfidence())
                .setSummary(evaluation.getSummary())
                .build();
        kafkaTemplate.send("vendor-events", evaluation.getVendorId(), event);
    }
}
