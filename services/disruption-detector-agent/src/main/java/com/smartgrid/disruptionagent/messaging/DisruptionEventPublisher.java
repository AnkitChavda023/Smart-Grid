package com.smartgrid.disruptionagent.messaging;

import com.smartgrid.commons.avro.disruption.DisruptionDetected;
import com.smartgrid.commons.avro.notification.NotificationEvent;
import com.smartgrid.commons.web.CorrelationContext;
import com.smartgrid.disruptionagent.domain.Disruption;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DisruptionEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public DisruptionEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishDetected(Disruption disruption) {
        DisruptionDetected event = DisruptionDetected.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setDisruptionId(disruption.getId().toString())
                .setAffectedSkus(disruption.getAffectedSkus())
                .setRegion(disruption.getRegion())
                .setConfidence(disruption.getConfidence())
                .setReasoningTrace(disruption.getReasoningTrace())
                .build();
        kafkaTemplate.send("disruption-detected", disruption.getId().toString(), event);
    }

    /** Publishes a notification event for disruptions requiring manual review. */
    public void publishReviewNotification(Disruption disruption) {
        NotificationEvent event = NotificationEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setNotificationId(UUID.randomUUID().toString())
                .setUserId("system")
                .setChannel("WEBSOCKET")
                .setTitle("Possible disruption needs review")
                .setBody("Vendor " + disruption.getVendorId() + " in " + disruption.getRegion()
                        + " shows anomaly signals but confidence (" + disruption.getConfidence() + ") is below the auto-publish threshold.")
                .setRelatedEntityId(disruption.getId().toString())
                .build();
        kafkaTemplate.send("notifications", disruption.getId().toString(), event);
    }
}
