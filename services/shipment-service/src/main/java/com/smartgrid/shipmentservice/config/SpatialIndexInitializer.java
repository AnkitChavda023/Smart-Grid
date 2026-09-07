package com.smartgrid.shipmentservice.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@DependsOn("entityManagerFactory")
public class SpatialIndexInitializer {

    private final JdbcTemplate jdbcTemplate;

    public SpatialIndexInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureIndexesExist() {
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_warehouses_location ON warehouses USING GIST(location)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_shipment_checkpoints_location ON shipment_checkpoints USING GIST(location)");
    }
}
