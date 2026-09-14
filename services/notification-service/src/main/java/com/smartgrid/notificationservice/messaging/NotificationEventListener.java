package com.smartgrid.notificationservice.messaging;

import com.smartgrid.commons.avro.disruption.DisruptionDetected;
import com.smartgrid.commons.avro.notification.NotificationEvent;
import com.smartgrid.commons.avro.order.OrderCreated;
import com.smartgrid.commons.avro.order.OrderFulfilled;
import com.smartgrid.commons.avro.reroute.EscalationRequired;
import com.smartgrid.commons.avro.reroute.RerouteDecision;
import com.smartgrid.commons.avro.shipment.ShipmentDelayed;
import com.smartgrid.commons.avro.sla.SLABreached;
import com.smartgrid.notificationservice.service.NotificationDispatchService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

    private final NotificationDispatchService dispatchService;

    public NotificationEventListener(NotificationDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @KafkaListener(topics = "order-events", groupId = "${smartgrid.kafka.group-id}-orders")
    public void onOrderEvent(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof OrderCreated created) {
            fanOut(created.getEventId(), "Order created",
                    "Order " + created.getOrderId() + " created for destination " + created.getDestinationRegion(),
                    created.getOrderId());
        } else if (record.value() instanceof OrderFulfilled fulfilled) {
            fanOut(fulfilled.getEventId(), "Order fulfilled",
                    "Order " + fulfilled.getOrderId() + " was fulfilled by vendor " + fulfilled.getVendorId(),
                    fulfilled.getOrderId());
        }
    }

    @KafkaListener(topics = "reroute-decisions", groupId = "${smartgrid.kafka.group-id}-reroute")
    public void onRerouteDecision(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof RerouteDecision decision) {
            fanOut(decision.getEventId(), "Reroute completed",
                    "Order " + decision.getOrderId() + " rerouted to vendor " + decision.getSelectedVendorId()
                            + " (confidence " + decision.getConfidence() + ")",
                    decision.getOrderId());
        } else if (record.value() instanceof EscalationRequired escalation) {
            fanOut(escalation.getEventId(), "Reroute needs human review",
                    "Order " + escalation.getOrderId() + " could not be confidently rerouted automatically ("
                            + escalation.getReason() + ", confidence " + escalation.getConfidence() + ")",
                    escalation.getOrderId());
        }
    }

    @KafkaListener(topics = "notifications", groupId = "${smartgrid.kafka.group-id}-generic")
    public void onGenericNotification(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof NotificationEvent event) {
            fanOut(event.getEventId(), event.getTitle(), event.getBody(), event.getRelatedEntityId());
        }
    }

    @KafkaListener(topics = "sla-events", groupId = "${smartgrid.kafka.group-id}-sla")
    public void onSlaEvent(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof SLABreached breached) {
            fanOut(breached.getEventId(), "SLA breach detected",
                    "Vendor " + breached.getVendorId() + " breached its SLA (" + breached.getSeverity() + ")",
                    null);
        }
    }

    @KafkaListener(topics = "shipment-events", groupId = "${smartgrid.kafka.group-id}-shipments")
    public void onShipmentEvent(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof ShipmentDelayed delayed) {
            fanOut(delayed.getEventId(), "Shipment delayed",
                    "Shipment for order " + delayed.getOrderId() + " delayed by " + delayed.getDelayMinutes() + " minute(s)",
                    delayed.getOrderId());
        }
    }

    @KafkaListener(topics = "disruption-detected", groupId = "${smartgrid.kafka.group-id}-disruption")
    public void onDisruptionDetected(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof DisruptionDetected detected) {
            fanOut(detected.getEventId(), "Disruption detected",
                    "Disruption detected in region " + detected.getRegion() + " affecting " + detected.getAffectedSkus().size() + " SKU(s)",
                    null);
        }
    }

    private void fanOut(String eventId, String title, String body, String relatedOrderId) {
        dispatchService.dispatchToAll(eventId, title, body, relatedOrderId);
    }
}
