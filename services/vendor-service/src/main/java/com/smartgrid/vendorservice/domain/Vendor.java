package com.smartgrid.vendorservice.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vendors")
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String region;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false)
    private String capabilities = "";

    @Column(nullable = false)
    private String contact = "";

    @Column(nullable = false)
    private String category = "Electronics";

    @Column(nullable = false)
    private String certifications = "ISO-9001";

    @Column(nullable = false)
    private boolean suspended = false;

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<VendorSku> skus = new ArrayList<>();

    protected Vendor() {
    }

    public Vendor(String name, String region, Double latitude, Double longitude, String capabilities) {
        this(name, region, latitude, longitude, capabilities, null, null, null);
    }

    public Vendor(String name, String region, Double latitude, Double longitude, String capabilities,
                  String contact, String category, String certifications) {
        this.name = name;
        this.region = region;
        this.latitude = latitude;
        this.longitude = longitude;
        this.capabilities = capabilities == null ? "" : capabilities;
        this.contact = (contact != null && !contact.isBlank())
                ? contact
                : "contact@" + (name != null ? name.toLowerCase().replaceAll("[^a-z0-9]", "-") : "vendor") + ".com";
        this.category = (category != null && !category.isBlank()) ? category : inferCategory(name, capabilities);
        this.certifications = (certifications != null && !certifications.isBlank()) ? certifications : "ISO-9001, RoHS";
    }

    private static String inferCategory(String name, String capabilities) {
        String combined = ((name != null ? name : "") + " " + (capabilities != null ? capabilities : "")).toLowerCase();
        if (combined.contains("pack") || combined.contains("box") || combined.contains("contain")) {
            return "Packaging";
        }
        if (combined.contains("raw") || combined.contains("metal") || combined.contains("steel") || combined.contains("plastic") || combined.contains("sheet")) {
            return "Raw Material";
        }
        if (combined.contains("hardware") || combined.contains("fasten") || combined.contains("machin") || combined.contains("precis")) {
            return "Precision Hardware";
        }
        return "Electronics";
    }

    public void addSku(VendorSku sku) {
        sku.setVendor(this);
        skus.add(sku);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRegion() {
        return region;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getCapabilities() {
        return capabilities;
    }

    public String getContact() {
        return (contact != null && !contact.isBlank())
                ? contact
                : "orders@" + (name != null ? name.toLowerCase().replaceAll("[^a-z0-9]", "-") : "vendor") + ".com";
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getCategory() {
        return (category != null && !category.isBlank()) ? category : inferCategory(name, capabilities);
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCertifications() {
        return (certifications != null && !certifications.isBlank()) ? certifications : "ISO-9001, RoHS";
    }

    public void setCertifications(String certifications) {
        this.certifications = certifications;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public List<VendorSku> getSkus() {
        return skus;
    }
}
