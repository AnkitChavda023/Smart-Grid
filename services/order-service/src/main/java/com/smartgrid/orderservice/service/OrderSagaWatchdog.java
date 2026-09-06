package com.smartgrid.orderservice.service;

import com.smartgrid.orderservice.domain.Order;
import com.smartgrid.orderservice.domain.OrderStatus;
import com.smartgrid.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Watchdog that monitors in-flight sagas.
 * If a service crashes or an external failure occurs mid-saga such that the order
 * remains PENDING beyond the timeout threshold, this watchdog automatically triggers
 * rollback compensation to ensure no leaked locks or unreleased inventory.
 */
@Component
@EnableScheduling
public class OrderSagaWatchdog {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaWatchdog.class);
    private static final long SAGA_TIMEOUT_SECONDS = 30;

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public OrderSagaWatchdog(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @Scheduled(fixedDelay = 10000)
    public void recoverTimedOutSagas() {
        Instant cutoff = Instant.now().minus(SAGA_TIMEOUT_SECONDS, ChronoUnit.SECONDS);
        List<Order> stuckOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, cutoff);

        for (Order order : stuckOrders) {
            if (!order.sagaComplete()) {
                log.warn("Saga Watchdog: Order {} timed out in PENDING (service crash recovery). Initiating rollback compensation...", order.getId());
                try {
                    orderService.cancelOrder(order.getId(), "Saga timed out mid-flight (crash recovery compensation)");
                } catch (Exception e) {
                    log.error("Failed to cancel timed-out order {}: {}", order.getId(), e.getMessage());
                }
            }
        }
    }
}
