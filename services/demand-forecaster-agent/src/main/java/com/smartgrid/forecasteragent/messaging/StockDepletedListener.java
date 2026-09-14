package com.smartgrid.forecasteragent.messaging;

import com.smartgrid.commons.avro.inventory.StockDepleted;
import com.smartgrid.forecasteragent.service.DemandForecastService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class StockDepletedListener {

    private final DemandForecastService forecastService;

    public StockDepletedListener(DemandForecastService forecastService) {
        this.forecastService = forecastService;
    }

    @KafkaListener(topics = "inventory-events", groupId = "${smartgrid.kafka.group-id}-stockout")
    public void onInventoryEvent(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof StockDepleted depleted) {
            forecastService.forecastForSku(depleted.getSkuId());
        }
    }
}
