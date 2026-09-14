package com.smartgrid.shipmentservice.web;

import com.smartgrid.shipmentservice.domain.Warehouse;
import com.smartgrid.shipmentservice.dto.WarehouseRequest;
import com.smartgrid.shipmentservice.dto.WarehouseResponse;
import com.smartgrid.shipmentservice.repository.WarehouseRepository;
import com.smartgrid.shipmentservice.service.GeoUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/warehouses")
public class WarehouseController {

    private final WarehouseRepository warehouseRepository;

    public WarehouseController(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    @PostMapping
    public ResponseEntity<WarehouseResponse> createWarehouse(@Valid @RequestBody WarehouseRequest request) {
        Warehouse saved = warehouseRepository.save(
                new Warehouse(request.name(), GeoUtil.point(request.latitude(), request.longitude())));
        return ResponseEntity.status(HttpStatus.CREATED).body(WarehouseResponse.from(saved));
    }

    @GetMapping("/nearest")
    public List<WarehouseResponse> nearest(
            @RequestParam double lat, @RequestParam double lon, @RequestParam(defaultValue = "1") int limit) {
        return warehouseRepository.findNearest(lat, lon, limit).stream().map(WarehouseResponse::from).toList();
    }
}
