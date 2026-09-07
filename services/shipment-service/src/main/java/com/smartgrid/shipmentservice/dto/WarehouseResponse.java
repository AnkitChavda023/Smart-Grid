package com.smartgrid.shipmentservice.dto;

import com.smartgrid.shipmentservice.domain.Warehouse;

import java.util.UUID;

public record WarehouseResponse(UUID id, String name, double latitude, double longitude) {

    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(warehouse.getId(), warehouse.getName(),
                warehouse.getLocation().getY(), warehouse.getLocation().getX());
    }
}
