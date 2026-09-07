package com.smartgrid.pricingservice.dto;

import com.smartgrid.pricingservice.domain.Quote;
import com.smartgrid.pricingservice.domain.QuoteStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuoteResponse(
        UUID id,
        String orderId,
        String vendorId,
        QuoteStatus status,
        double totalPrice,
        List<ItemResponse> items,
        Instant createdAt,
        Instant expiresAt
) {
    public record ItemResponse(String skuId, int quantity, double unitPrice) {
    }

    public static QuoteResponse from(Quote quote) {
        List<ItemResponse> items = quote.getItems().stream()
                .map(item -> new ItemResponse(item.getSkuId(), item.getQuantity(), item.getUnitPrice()))
                .toList();
        return new QuoteResponse(
                quote.getId(), quote.getOrderId(), quote.getVendorId(), quote.getStatus(),
                quote.getTotalPrice(), items, quote.getCreatedAt(), quote.getExpiresAt()
        );
    }
}
