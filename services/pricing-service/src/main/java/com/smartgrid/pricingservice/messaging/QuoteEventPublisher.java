package com.smartgrid.pricingservice.messaging;

import com.smartgrid.commons.avro.quote.QuoteAccepted;
import com.smartgrid.commons.avro.quote.QuoteCreated;
import com.smartgrid.commons.avro.quote.QuoteExpired;
import com.smartgrid.commons.web.CorrelationContext;
import com.smartgrid.pricingservice.domain.Quote;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class QuoteEventPublisher {

    private static final String TOPIC = "quote-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public QuoteEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishQuoteCreated(Quote quote) {
        var firstItem = quote.getItems().get(0);
        QuoteCreated event = QuoteCreated.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setQuoteId(quote.getId().toString())
                .setOrderId(quote.getOrderId())
                .setVendorId(quote.getVendorId())
                .setSkuId(firstItem.getSkuId())
                .setQuantity(firstItem.getQuantity())
                .setPrice(quote.getTotalPrice())
                .setExpiresAt(quote.getExpiresAt().toEpochMilli())
                .build();
        kafkaTemplate.send(TOPIC, quote.getOrderId(), event);
    }

    public void publishQuoteAccepted(Quote quote) {
        QuoteAccepted event = QuoteAccepted.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setQuoteId(quote.getId().toString())
                .setOrderId(quote.getOrderId())
                .build();
        kafkaTemplate.send(TOPIC, quote.getOrderId(), event);
    }

    public void publishQuoteExpired(Quote quote) {
        QuoteExpired event = QuoteExpired.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setQuoteId(quote.getId().toString())
                .build();
        kafkaTemplate.send(TOPIC, quote.getOrderId(), event);
    }
}
