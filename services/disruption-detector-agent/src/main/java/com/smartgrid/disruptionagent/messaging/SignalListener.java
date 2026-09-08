package com.smartgrid.disruptionagent.messaging;

import com.smartgrid.commons.avro.sla.SLABreached;
import com.smartgrid.commons.avro.shipment.ShipmentDelayed;
import com.smartgrid.commons.avro.vendor.VendorScoreUpdated;
import com.smartgrid.commons.avro.vendor.VendorSuspended;
import com.smartgrid.disruptionagent.client.OrderClient;
import com.smartgrid.disruptionagent.config.DisruptionProperties;
import com.smartgrid.disruptionagent.service.DisruptionAnalysisService;
import com.smartgrid.disruptionagent.window.Signal;
import com.smartgrid.disruptionagent.window.SignalType;
import com.smartgrid.disruptionagent.window.SlidingWindowStore;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Component
public class SignalListener {

    private static final Logger log = LoggerFactory.getLogger(SignalListener.class);

    private final SlidingWindowStore windowStore;
    private final OrderClient orderClient;
    private final DisruptionAnalysisService analysisService;
    private final DisruptionProperties properties;

    public SignalListener(SlidingWindowStore windowStore, OrderClient orderClient,
                           DisruptionAnalysisService analysisService, DisruptionProperties properties) {
        this.windowStore = windowStore;
        this.orderClient = orderClient;
        this.analysisService = analysisService;
        this.properties = properties;
    }

    @KafkaListener(topics = "vendor-events", groupId = "${smartgrid.kafka.group-id}-vendor")
    public void onVendorEvent(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof VendorScoreUpdated event) {
            double drop = event.getPreviousScore() - event.getNewScore();
            if (drop >= properties.scoreDropThreshold()) {
                observe(event.getVendorId(), SignalType.SCORE_DROP, drop,
                        "Vendor score dropped from " + event.getPreviousScore() + " to " + event.getNewScore() + ": " + event.getReason());
            }
        } else if (record.value() instanceof VendorSuspended event) {
            observe(event.getVendorId(), SignalType.VENDOR_SUSPENSION, 1.0, "Vendor suspended: " + event.getReason());
        }
    }

    @KafkaListener(topics = "sla-events", groupId = "${smartgrid.kafka.group-id}-sla")
    public void onSlaEvent(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof SLABreached event) {
            observe(event.getVendorId(), SignalType.SLA_BREACH, event.getPenaltyAmount(),
                    "SLA breach (" + event.getSeverity() + "), penalty " + event.getPenaltyAmount());
        }
    }

    @KafkaListener(topics = "shipment-events", groupId = "${smartgrid.kafka.group-id}-shipment")
    public void onShipmentEvent(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof ShipmentDelayed event) {
            // ShipmentDelayed only carries orderId, not vendorId — resolve it via the order it belongs to.
            orderClient.getVendorId(event.getOrderId()).ifPresent(vendorId ->
                    observe(vendorId, SignalType.SHIPMENT_DELAY, event.getDelayMinutes(),
                            "Shipment for order " + event.getOrderId() + " delayed " + event.getDelayMinutes() + " min: " + event.getReason()));
        }
    }

    public void observe(String vendorId, SignalType type, double severity, String description) {
        Signal signal = new Signal(Instant.now(), type, severity, description);
        windowStore.record(vendorId, signal, Duration.ofMinutes(properties.windowMinutes()));

        List<Signal> signals = windowStore.signalsInWindow(vendorId, Duration.ofMinutes(properties.windowMinutes()));
        Set<SignalType> distinctTypes = windowStore.distinctSignalTypes(vendorId, Duration.ofMinutes(properties.windowMinutes()));

        // Per PR-09: When two or more correlated anomaly signals appear from the same vendor in a 15-minute window
        if (signals.size() >= 2 || distinctTypes.size() >= properties.minDistinctSignals()) {
            // Analysis calls vendor-service by UUID; older non-UUID vendorId formats from earlier test
            // data can still show up on the real topics and must not crash the listener
            if (!isUuid(vendorId)) {
                log.warn("Anomaly threshold reached for non-UUID vendorId={}, skipping analysis (stale/synthetic test data)", vendorId);
                return;
            }
            log.info("Anomaly threshold reached for vendorId={} totalSignals={} distinctTypes={}", vendorId, signals.size(), distinctTypes);
            analysisService.analyzeAndPublish(vendorId, signals);
        }
    }

    private boolean isUuid(String value) {
        try {
            java.util.UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
