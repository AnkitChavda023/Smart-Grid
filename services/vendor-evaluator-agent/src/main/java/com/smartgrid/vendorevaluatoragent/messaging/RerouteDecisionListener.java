package com.smartgrid.vendorevaluatoragent.messaging;

import com.smartgrid.commons.avro.reroute.RerouteDecision;
import com.smartgrid.vendorevaluatoragent.service.VendorEvaluationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RerouteDecisionListener {

    private final VendorEvaluationService evaluationService;

    public RerouteDecisionListener(VendorEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @KafkaListener(topics = "reroute-decisions", groupId = "${smartgrid.kafka.group-id}-reroute")
    public void onRerouteDecision(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof RerouteDecision decision && decision.getSelectedVendorId() != null) {
            evaluationService.evaluate(decision.getSelectedVendorId());
        }
    }
}
