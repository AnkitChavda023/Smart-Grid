package com.smartgrid.contractservice.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "contracts")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vendor_id", nullable = false)
    private String vendorId;

    @Column(nullable = false, columnDefinition = "text")
    private String terms;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "expiry_notified", nullable = false)
    private boolean expiryNotified = false;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true, fetch = jakarta.persistence.FetchType.EAGER)
    private List<SlaTerm> slaTerms = new ArrayList<>();

    protected Contract() {
    }

    public Contract(String vendorId, String terms, LocalDate startDate, LocalDate endDate) {
        this.vendorId = vendorId;
        this.terms = terms;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void addSlaTerm(SlaTerm term) {
        term.setContract(this);
        slaTerms.add(term);
    }

    public UUID getId() {
        return id;
    }

    public String getVendorId() {
        return vendorId;
    }

    public String getTerms() {
        return terms;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isExpiryNotified() {
        return expiryNotified;
    }

    public void setExpiryNotified(boolean expiryNotified) {
        this.expiryNotified = expiryNotified;
    }

    public List<SlaTerm> getSlaTerms() {
        return slaTerms;
    }
}
