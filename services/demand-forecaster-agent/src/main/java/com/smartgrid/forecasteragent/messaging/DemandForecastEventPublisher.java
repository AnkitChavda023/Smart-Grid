package com.smartgrid.forecasteragent.messaging;

import com.smartgrid.commons.avro.demand.DemandForecastGenerated;
import com.smartgrid.commons.web.CorrelationContext;
import com.smartgrid.forecasteragent.domain.DemandForecast;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DemandForecastEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public DemandForecastEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(DemandForecast forecast) {
        DemandForecastGenerated event = DemandForecastGenerated.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setSkuId(forecast.getSkuId())
                .setHorizonDays(forecast.getHorizonDays())
                .setP10(forecast.getP10())
                .setP50(forecast.getP50())
                .setP90(forecast.getP90())
                .build();
        kafkaTemplate.send("inventory-events", forecast.getSkuId(), event);
    }
}
