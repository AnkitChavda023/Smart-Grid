package com.smartgrid.orderservice.dto;

import com.smartgrid.orderservice.domain.Order;
import com.smartgrid.orderservice.domain.OrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        OrderStatus status,
        long version,
        String requestedBy,
        String destinationRegion,
        List<OrderItemResponse> items,
        String vendorId,
        String quoteId,
        Instant createdAt
) {

    public record OrderItemResponse(String skuId, int quantity) {
    }

    public static OrderResponse from(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(item.getSkuId(), item.getQuantity()))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getVersion(),
                order.getRequestedBy(),
                order.getDestinationRegion(),
                items,
                order.getVendorId(),
                order.getQuoteId(),
                order.getCreatedAt()
        );
    }
}
