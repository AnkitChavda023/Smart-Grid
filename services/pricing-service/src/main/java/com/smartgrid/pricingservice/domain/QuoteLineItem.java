package com.smartgrid.pricingservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "quote_line_items")
public class QuoteLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @Column(name = "sku_id", nullable = false)
    private String skuId;

    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private double unitPrice;

    protected QuoteLineItem() {
    }

    public QuoteLineItem(String skuId, int quantity, double unitPrice) {
        this.skuId = skuId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public void setQuote(Quote quote) {
        this.quote = quote;
    }

    public UUID getId() {
        return id;
    }

    public String getSkuId() {
        return skuId;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }
}
