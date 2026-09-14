package com.smartgrid.rerouteagent.messaging;

import com.smartgrid.commons.avro.disruption.DisruptionDetected;
import com.smartgrid.rerouteagent.client.OrderClient;
import com.smartgrid.rerouteagent.service.RerouteOrchestrationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DisruptionDetectedListener {

    private static final Logger log = LoggerFactory.getLogger(DisruptionDetectedListener.class);

    private final OrderClient orderClient;
    private final RerouteOrchestrationService orchestrationService;

    public DisruptionDetectedListener(OrderClient orderClient, RerouteOrchestrationService orchestrationService) {
        this.orderClient = orderClient;
        this.orchestrationService = orchestrationService;
    }

    @KafkaListener(topics = "disruption-detected", groupId = "${smartgrid.kafka.group-id}-disruption")
    public void onDisruptionDetected(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof DisruptionDetected event) {
            var affectedOrders = orderClient.findReroutableOrders(event.getRegion(), event.getAffectedSkus());
            log.info("DisruptionDetected id={} region={} skus={} -> {} reroutable order(s)",
                    event.getDisruptionId(), event.getRegion(), event.getAffectedSkus(), affectedOrders.size());
            for (var order : affectedOrders) {
                orchestrationService.planForOrder(order.id(), event.getDisruptionId(), event.getRegion(), event.getAffectedSkus());
            }
        }
    }
}
