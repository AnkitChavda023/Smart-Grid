package com.smartgrid.orderservice.web;

import com.smartgrid.commons.dto.PageResponse;
import com.smartgrid.orderservice.domain.Order;
import com.smartgrid.orderservice.domain.OrderStatus;
import com.smartgrid.orderservice.dto.CreateOrderRequest;
import com.smartgrid.orderservice.dto.OrderResponse;
import com.smartgrid.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable UUID id) {
        return OrderResponse.from(orderService.getOrder(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body
    ) {
        String statusStr = body.get("status");
        if (statusStr == null || statusStr.isBlank()) {
            throw new IllegalArgumentException("Target status is required");
        }
        OrderStatus target = OrderStatus.valueOf(statusStr.toUpperCase());
        Order updated = orderService.transitionOrder(id, target);
        return ResponseEntity.ok(OrderResponse.from(updated));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? "Cancelled by request" : body.getOrDefault("reason", "Cancelled by request");
        orderService.cancelOrder(id, reason);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/affected")
    public List<OrderResponse> reroutableOrders(
            @RequestParam String region,
            @RequestParam List<String> skus
    ) {
        return orderService.findReroutableOrders(region, skus).stream().map(OrderResponse::from).toList();
    }

    @GetMapping
    public PageResponse<OrderResponse> listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orders = orderService.listOrders(status, pageable);
        return PageResponse.of(orders.getContent().stream().map(OrderResponse::from).toList(),
                page, size, orders.getTotalElements());
    }

    @GetMapping("/analytics/summary")
    public OrderAnalyticsSummary getAnalyticsSummary() {
        java.time.Instant now = java.time.Instant.now();
        java.time.Instant startOfToday = now.minus(java.time.Duration.ofDays(1));
        java.time.Instant startOfWeek = now.minus(java.time.Duration.ofDays(7));
        java.time.Instant startOfMonth = now.minus(java.time.Duration.ofDays(30));

        long today = orderService.countOrdersCreatedAfter(startOfToday);
        long thisWeek = orderService.countOrdersCreatedAfter(startOfWeek);
        long thisMonth = orderService.countOrdersCreatedAfter(startOfMonth);
        long total = orderService.countTotalOrders();

        return new OrderAnalyticsSummary(today, thisWeek, thisMonth, total);
    }

    public record OrderAnalyticsSummary(long today, long thisWeek, long thisMonth, long total) {
    }
}
