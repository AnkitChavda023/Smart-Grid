package com.smartgrid.orderservice.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Version
    private long version;

    @Column(nullable = false)
    private String requestedBy;

    @Column(nullable = false)
    private String destinationRegion;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    private String vendorId;

    private String quoteId;

    @Column(nullable = false)
    private boolean reservationConfirmed = false;

    @Column(nullable = false)
    private boolean vendorConfirmed = false;

    @Column(nullable = false)
    private boolean quoteAccepted = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Order() {
    }

    public Order(String requestedBy, String destinationRegion) {
        this.requestedBy = requestedBy;
        this.destinationRegion = destinationRegion;
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public boolean sagaComplete() {
        return reservationConfirmed && vendorConfirmed && quoteAccepted;
    }

    public UUID getId() {
        return id;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public long getVersion() {
        return version;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public String getDestinationRegion() {
        return destinationRegion;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getQuoteId() {
        return quoteId;
    }

    public void setQuoteId(String quoteId) {
        this.quoteId = quoteId;
    }

    public boolean isReservationConfirmed() {
        return reservationConfirmed;
    }

    public void setReservationConfirmed(boolean reservationConfirmed) {
        this.reservationConfirmed = reservationConfirmed;
    }

    public boolean isVendorConfirmed() {
        return vendorConfirmed;
    }

    public void setVendorConfirmed(boolean vendorConfirmed) {
        this.vendorConfirmed = vendorConfirmed;
    }

    public boolean isQuoteAccepted() {
        return quoteAccepted;
    }

    public void setQuoteAccepted(boolean quoteAccepted) {
        this.quoteAccepted = quoteAccepted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
