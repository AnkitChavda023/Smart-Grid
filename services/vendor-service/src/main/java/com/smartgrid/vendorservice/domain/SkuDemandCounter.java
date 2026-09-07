package com.smartgrid.vendorservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sku_demand_counters")
public class SkuDemandCounter {

    @Id
    @Column(name = "sku_id")
    private String skuId;

    @Column(name = "total_quantity", nullable = false)
    private long totalQuantity;

    protected SkuDemandCounter() {
    }

    public SkuDemandCounter(String skuId, long totalQuantity) {
        this.skuId = skuId;
        this.totalQuantity = totalQuantity;
    }

    public String getSkuId() {
        return skuId;
    }

    public long getTotalQuantity() {
        return totalQuantity;
    }

    public void addQuantity(int quantity) {
        this.totalQuantity += quantity;
    }
}
