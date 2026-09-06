package com.smartgrid.orderservice.service;

import com.smartgrid.commons.avro.order.OrderCancelled;
import com.smartgrid.commons.avro.order.OrderCreated;
import com.smartgrid.commons.avro.order.OrderFulfilled;
import com.smartgrid.commons.avro.order.OrderLineItem;
import com.smartgrid.commons.exception.ResourceNotFoundException;
import com.smartgrid.commons.web.CorrelationContext;
import com.smartgrid.orderservice.domain.Order;
import com.smartgrid.orderservice.domain.OrderItem;
import com.smartgrid.orderservice.domain.OrderStateMachine;
import com.smartgrid.orderservice.domain.OrderStatus;
import com.smartgrid.orderservice.dto.CreateOrderRequest;
import com.smartgrid.orderservice.messaging.OutboxPublisher;
import com.smartgrid.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OutboxPublisher outboxPublisher;

    public OrderService(OrderRepository orderRepository, OutboxPublisher outboxPublisher) {
        this.orderRepository = orderRepository;
        this.outboxPublisher = outboxPublisher;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Order order = new Order(request.requestedBy(), request.destinationRegion());
        request.items().forEach(item -> order.addItem(new OrderItem(item.skuId(), item.quantity())));
        Order saved = orderRepository.save(order);

        OrderCreated event = OrderCreated.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setOrderId(saved.getId().toString())
                .setRequestedBy(saved.getRequestedBy())
                .setDestinationRegion(saved.getDestinationRegion())
                .setItems(saved.getItems().stream()
                        .map(item -> OrderLineItem.newBuilder().setSkuId(item.getSkuId()).setQuantity(item.getQuantity()).build())
                        .toList())
                .build();
        outboxPublisher.publish(saved.getId().toString(), "OrderCreated", event);

        return saved;
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id.toString()));
    }

    @Transactional
    public void cancelOrder(UUID id, String reason) {
        Order order = getOrder(id);
        OrderStateMachine.assertValidTransition(order.getStatus(), OrderStatus.CANCELLED);
        order.setStatus(OrderStatus.CANCELLED);

        OrderCancelled event = OrderCancelled.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(System.currentTimeMillis())
                .setCorrelationId(CorrelationContext.get())
                .setOrderId(id.toString())
                .setReason(reason)
                .build();
        outboxPublisher.publish(id.toString(), "OrderCancelled", event);
        notifyOrderStateChange(id.toString(), OrderStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public Page<Order> listOrders(String statusFilter, Pageable pageable) {
        if (statusFilter == null || statusFilter.equalsIgnoreCase("ACTIVE")) {
            return orderRepository.findByStatusNotIn(List.of(OrderStatus.CLOSED, OrderStatus.CANCELLED), pageable);
        }
        if (statusFilter.equalsIgnoreCase("ALL")) {
            return orderRepository.findAll(pageable);
        }
        return orderRepository.findByStatus(OrderStatus.valueOf(statusFilter.toUpperCase()), pageable);
    }

    /** In-flight orders (not yet shipped) for a region carrying any of the given SKUs. */
    @Transactional(readOnly = true)
    public List<Order> findReroutableOrders(String destinationRegion, List<String> skuIds) {
        return orderRepository.findDistinctByDestinationRegionAndItems_SkuIdInAndStatusIn(
                destinationRegion, skuIds, List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED));
    }

    @Transactional
    public void onReservationConfirmed(String orderId) {
        withOrder(orderId, order -> {
            order.setReservationConfirmed(true);
            completeSagaIfReady(order);
        });
    }

    @Transactional
    public void onVendorConfirmed(String orderId, String vendorId) {
        withOrder(orderId, order -> {
            order.setVendorConfirmed(true);
            order.setVendorId(vendorId);
            completeSagaIfReady(order);
        });
    }

    @Transactional
    public void onQuoteAccepted(String orderId, String quoteId) {
        withOrder(orderId, order -> {
            order.setQuoteAccepted(true);
            order.setQuoteId(quoteId);
            completeSagaIfReady(order);
        });
    }

    @Transactional
    public void onRerouteDecision(String orderId, String vendorId, String quoteId) {
        withOrder(orderId, order -> {
            order.setVendorId(vendorId);
            order.setQuoteId(quoteId);
            orderRepository.save(order);
            notifyOrderStateChange(order.getId().toString(), order.getStatus());
        });
    }

    @Transactional
    public Order transitionOrder(UUID id, OrderStatus targetStatus) {
        Order order = getOrder(id);
        OrderStateMachine.assertValidTransition(order.getStatus(), targetStatus);
        order.setStatus(targetStatus);
        Order saved = orderRepository.save(order);
        notifyOrderStateChange(saved.getId().toString(), targetStatus);
        return saved;
    }

    private void withOrder(String orderId, java.util.function.Consumer<Order> action) {
        UUID id;
        try {
            id = UUID.fromString(orderId);
        } catch (IllegalArgumentException e) {
            log.warn("Ignoring event for non-UUID orderId={}", orderId);
            return;
        }
        orderRepository.findById(id).ifPresent(action);
    }

    private void completeSagaIfReady(Order order) {
        if (order.getStatus() == OrderStatus.PENDING && order.sagaComplete()) {
            order.setStatus(OrderStatus.CONFIRMED);

            OrderFulfilled event = OrderFulfilled.newBuilder()
                    .setEventId(UUID.randomUUID().toString())
                    .setOccurredAt(System.currentTimeMillis())
                    .setCorrelationId(CorrelationContext.get())
                    .setOrderId(order.getId().toString())
                    .setVendorId(order.getVendorId())
                    .setQuoteId(order.getQuoteId())
                    .setDestinationRegion(order.getDestinationRegion())
                    .build();
            outboxPublisher.publish(order.getId().toString(), "OrderFulfilled", event);
            notifyOrderStateChange(order.getId().toString(), OrderStatus.CONFIRMED);
        }
    }

    private void notifyOrderStateChange(String orderId, OrderStatus status) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            String json = String.format("{\"title\":\"Order status updated\",\"body\":\"Order %s is now %s\",\"relatedEntityId\":\"%s\"}",
                    orderId, status, orderId);
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8084/notifications/adhoc"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .build();
            client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.warn("Failed to push real-time notification for order {}: {}", orderId, e.getMessage());
        }
    }

    public long countOrdersCreatedAfter(java.time.Instant timestamp) {
        return orderRepository.countByCreatedAtAfter(timestamp);
    }

    public long countTotalOrders() {
        return orderRepository.count();
    }
}
