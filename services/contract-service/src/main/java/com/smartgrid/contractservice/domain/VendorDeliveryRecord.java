package com.smartgrid.contractservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "vendor_delivery_records")
public class VendorDeliveryRecord {

    @Id
    @Column(name = "order_id")
    private String orderId;

    @Column(name = "vendor_id", nullable = false)
    private String vendorId;

    @Column(name = "fulfilled_at", nullable = false)
    private Instant fulfilledAt;

    @Column(nullable = false)
    private boolean delivered = false;

    protected VendorDeliveryRecord() {
    }

    public VendorDeliveryRecord(String orderId, String vendorId, Instant fulfilledAt) {
        this.orderId = orderId;
        this.vendorId = vendorId;
        this.fulfilledAt = fulfilledAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getVendorId() {
        return vendorId;
    }

    public Instant getFulfilledAt() {
        return fulfilledAt;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }
}
