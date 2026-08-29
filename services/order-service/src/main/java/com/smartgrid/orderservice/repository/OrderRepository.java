package com.smartgrid.orderservice.repository;

import com.smartgrid.orderservice.domain.Order;
import com.smartgrid.orderservice.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    Page<Order> findByStatusNotIn(Iterable<OrderStatus> statuses, Pageable pageable);

    List<Order> findDistinctByDestinationRegionAndItems_SkuIdInAndStatusIn(
            String destinationRegion, List<String> skuIds, List<OrderStatus> statuses);
}
