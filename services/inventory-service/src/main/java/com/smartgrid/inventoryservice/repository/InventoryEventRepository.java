package com.smartgrid.inventoryservice.repository;

import com.smartgrid.inventoryservice.domain.InventoryEvent;
import com.smartgrid.inventoryservice.domain.InventoryEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryEventRepository extends JpaRepository<InventoryEvent, Long> {

    List<InventoryEvent> findBySkuIdAndWarehouseIdOrderByIdAsc(String skuId, String warehouseId);

    List<InventoryEvent> findByOrderIdAndEventType(String orderId, InventoryEventType eventType);

    List<InventoryEvent> findBySkuIdAndEventTypeOrderByCreatedAtAsc(String skuId, InventoryEventType eventType);

    @Query("SELECT MAX(e.id) FROM InventoryEvent e WHERE e.skuId = :skuId AND e.warehouseId = :warehouseId")
    Long findMaxEventId(@Param("skuId") String skuId, @Param("warehouseId") String warehouseId);
}
