package com.smartgrid.inventoryservice.messaging;

import com.smartgrid.commons.avro.order.OrderCancelled;
import com.smartgrid.commons.avro.order.OrderCreated;
import com.smartgrid.commons.avro.order.OrderLineItem;
import com.smartgrid.commons.exception.ConflictException;
import com.smartgrid.inventoryservice.domain.InventorySnapshot;
import com.smartgrid.inventoryservice.repository.InventorySnapshotRepository;
import com.smartgrid.inventoryservice.service.InventoryService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class InventoryOrderListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryOrderListener.class);

    private final InventoryService inventoryService;
    private final InventorySnapshotRepository snapshotRepository;

    public InventoryOrderListener(InventoryService inventoryService, InventorySnapshotRepository snapshotRepository) {
        this.inventoryService = inventoryService;
        this.snapshotRepository = snapshotRepository;
    }

    @KafkaListener(topics = "order-events", groupId = "${smartgrid.kafka.group-id}-orders")
    public void onOrderEvent(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof OrderCreated created) {
            for (OrderLineItem item : created.getItems()) {
                reserveFromBestWarehouse(created.getOrderId(), item);
            }
        } else if (record.value() instanceof OrderCancelled cancelled) {
            inventoryService.releaseAllForOrder(cancelled.getOrderId());
        }
    }

    private void reserveFromBestWarehouse(String orderId, OrderLineItem item) {
        List<InventorySnapshot> candidates = snapshotRepository.findBySkuId(item.getSkuId());
        InventorySnapshot best = candidates.stream()
                .filter(s -> s.getAvailableQuantity() >= item.getQuantity())
                .max(Comparator.comparingLong(InventorySnapshot::getAvailableQuantity))
                .orElse(null);

        if (best == null) {
            log.warn("No warehouse has sufficient stock for order={} sku={} quantity={}",
                    orderId, item.getSkuId(), item.getQuantity());
            cancelOrderFromSaga(orderId, "Inventory reservation failed: insufficient stock for SKU " + item.getSkuId());
            return;
        }

        try {
            inventoryService.reserve(orderId, item.getSkuId(), best.getWarehouseId(), item.getQuantity());
        } catch (ConflictException e) {
            log.warn("Reservation race lost for order={} sku={} warehouse={}", orderId, item.getSkuId(), best.getWarehouseId());
            cancelOrderFromSaga(orderId, "Inventory reservation failed: concurrent reservation race for SKU " + item.getSkuId());
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
            java.net.http.HttpResponse<String> resp = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            log.info("Rollback compensation: cancelled order {} from saga (HTTP {})", orderId, resp.statusCode());
        } catch (Exception e) {
            log.warn("Failed to notify order-service of cancellation for order {}: {}", orderId, e.getMessage());
        }
    }
}
