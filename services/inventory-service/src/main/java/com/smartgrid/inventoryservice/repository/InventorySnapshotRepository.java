package com.smartgrid.inventoryservice.repository;

import com.smartgrid.inventoryservice.domain.InventorySnapshot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventorySnapshotRepository extends JpaRepository<InventorySnapshot, UUID> {

    List<InventorySnapshot> findBySkuId(String skuId);

    List<InventorySnapshot> findByWarehouseId(String warehouseId);

    Optional<InventorySnapshot> findBySkuIdAndWarehouseId(String skuId, String warehouseId);

    @Query("SELECT DISTINCT s.skuId FROM InventorySnapshot s")
    List<String> findDistinctSkuIds();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InventorySnapshot s WHERE s.skuId = :skuId AND s.warehouseId = :warehouseId")
    Optional<InventorySnapshot> findForUpdate(@Param("skuId") String skuId, @Param("warehouseId") String warehouseId);
}
