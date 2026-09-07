package com.smartgrid.pricingservice.messaging;

import com.smartgrid.commons.avro.vendor.VendorConfirmed;
import com.smartgrid.commons.avro.vendor.VendorScoreUpdated;
import com.smartgrid.pricingservice.domain.Quote;
import com.smartgrid.pricingservice.dto.CreateQuoteRequest;
import com.smartgrid.pricingservice.dto.QuoteItemRequest;
import com.smartgrid.pricingservice.service.PriceRuleAdjustmentService;
import com.smartgrid.pricingservice.service.QuoteService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PricingVendorEventListener {

    private static final Logger log = LoggerFactory.getLogger(PricingVendorEventListener.class);

    private final PriceRuleAdjustmentService adjustmentService;
    private final QuoteService quoteService;

    public PricingVendorEventListener(PriceRuleAdjustmentService adjustmentService, QuoteService quoteService) {
        this.adjustmentService = adjustmentService;
        this.quoteService = quoteService;
    }

    @KafkaListener(topics = "vendor-events", groupId = "${smartgrid.kafka.group-id}-vendor")
    public void onVendorEvent(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof VendorScoreUpdated updated && updated.getSkuId() != null) {
            adjustmentService.recalculateForVendorScoreChange(updated.getSkuId(), updated.getPreviousScore(), updated.getNewScore());
        } else if (record.value() instanceof VendorConfirmed confirmed) {
            handleVendorConfirmed(confirmed);
        }
    }

    private void handleVendorConfirmed(VendorConfirmed confirmed) {
        String orderId = confirmed.getOrderId().toString();
        String vendorId = confirmed.getVendorId().toString();
        String skuId = confirmed.getSkuId().toString();
        int quantity = confirmed.getQuantity();

        try {
            CreateQuoteRequest request = new CreateQuoteRequest(
                    orderId,
                    vendorId,
                    List.of(new QuoteItemRequest(skuId, quantity))
            );
            Quote quote = quoteService.createQuote(request);
            quoteService.acceptQuote(quote.getId());
            log.info("Order saga: auto-generated and atomically accepted quote {} for order {}", quote.getId(), orderId);
        } catch (Exception e) {
            log.error("Order saga: failed to create or accept quote for order {}: {}", orderId, e.getMessage());
            cancelOrderFromSaga(orderId, "Quote generation or acceptance failed: " + e.getMessage());
        }
    }

    private void cancelOrderFromSaga(String orderId, String reason) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            String json = String.format("{\"reason\":\"%s\"}", reason.replace("\"", "\\\""));
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8181/orders/" + orderId + "/cancel"))
                    .header("Content-Type", "application/json")
                    .method("PATCH", java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .build();
            client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.warn("Failed to notify order-service of cancellation for order {}: {}", orderId, e.getMessage());
        }
    }
}
