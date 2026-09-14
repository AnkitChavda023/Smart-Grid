package com.smartgrid.shipmentservice.repository;

import com.smartgrid.shipmentservice.domain.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {

    // The <-> operator is PostGIS's index-accelerated K-nearest-neighbor distance operator: with a GiST
    // index on `location`, Postgres walks the index instead of computing ST_Distance for every row.
    @Query(value = "SELECT * FROM warehouses ORDER BY location <-> ST_SetSRID(ST_MakePoint(:lon, :lat), 4326) LIMIT :limit",
            nativeQuery = true)
    List<Warehouse> findNearest(@Param("lat") double lat, @Param("lon") double lon, @Param("limit") int limit);
}
