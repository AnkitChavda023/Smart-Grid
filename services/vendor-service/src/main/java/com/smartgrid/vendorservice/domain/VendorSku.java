package com.smartgrid.vendorservice.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "vendor_skus")
public class VendorSku {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @jakarta.persistence.Column(name = "sku_id", nullable = false)
    private String skuId;

    private double price;

    private int leadTimeDays;

    protected VendorSku() {
    }

    public VendorSku(String skuId, double price, int leadTimeDays) {
        this.skuId = skuId;
        this.price = price;
        this.leadTimeDays = leadTimeDays;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public UUID getId() {
        return id;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public String getSkuId() {
        return skuId;
    }

    public double getPrice() {
        return price;
    }

    public int getLeadTimeDays() {
        return leadTimeDays;
    }
}
