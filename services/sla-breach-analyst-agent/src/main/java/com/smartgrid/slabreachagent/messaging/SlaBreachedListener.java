package com.smartgrid.slabreachagent.messaging;

import com.smartgrid.commons.avro.sla.SLABreached;
import com.smartgrid.slabreachagent.service.BreachRiskAssessmentService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SlaBreachedListener {

    private final BreachRiskAssessmentService assessmentService;

    public SlaBreachedListener(BreachRiskAssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @KafkaListener(topics = "sla-events", groupId = "${smartgrid.kafka.group-id}-breach")
    public void onSlaBreached(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof SLABreached breached) {
            assessmentService.assess(breached.getVendorId());
        }
    }
}
