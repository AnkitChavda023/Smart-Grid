package com.smartgrid.orderservice.messaging;

import com.smartgrid.commons.avro.inventory.ReservationConfirmed;
import com.smartgrid.commons.avro.quote.QuoteAccepted;
import com.smartgrid.commons.avro.reroute.RerouteDecision;
import com.smartgrid.commons.avro.vendor.VendorConfirmed;
import com.smartgrid.orderservice.service.OrderService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderSagaListener {

    private final OrderService orderService;

    public OrderSagaListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = "inventory-events", groupId = "${smartgrid.kafka.group-id}-inventory")
    public void onInventoryEvent(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof ReservationConfirmed confirmed) {
            orderService.onReservationConfirmed(confirmed.getOrderId());
        }
    }

    @KafkaListener(topics = "vendor-events", groupId = "${smartgrid.kafka.group-id}-vendor")
    public void onVendorEvent(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof VendorConfirmed confirmed) {
            orderService.onVendorConfirmed(confirmed.getOrderId(), confirmed.getVendorId());
        }
    }

    @KafkaListener(topics = "quote-events", groupId = "${smartgrid.kafka.group-id}-quote")
    public void onQuoteEvent(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof QuoteAccepted accepted) {
            orderService.onQuoteAccepted(accepted.getOrderId(), accepted.getQuoteId());
        }
    }

    @KafkaListener(topics = "reroute-decisions", groupId = "${smartgrid.kafka.group-id}-reroute")
    public void onRerouteDecision(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof RerouteDecision decision) {
            orderService.onRerouteDecision(decision.getOrderId(), decision.getSelectedVendorId(), decision.getQuoteId());
        }
    }
}
