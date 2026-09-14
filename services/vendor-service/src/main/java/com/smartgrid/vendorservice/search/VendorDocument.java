package com.smartgrid.vendorservice.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

@Document(indexName = "vendor-catalog")
public class VendorDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Keyword)
    private String region;

    @Field(type = FieldType.Text)
    private String capabilities;

    @Field(type = FieldType.Text)
    private String contact;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Text)
    private String certifications;

    @GeoPointField
    private GeoPoint location;

    protected VendorDocument() {
    }

    public VendorDocument(String id, String name, String region, String capabilities, String contact,
                          String category, String certifications, GeoPoint location) {
        this.id = id;
        this.name = name;
        this.region = region;
        this.capabilities = capabilities;
        this.contact = contact;
        this.category = category;
        this.certifications = certifications;
        this.location = location;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRegion() {
        return region;
    }

    public String getCapabilities() {
        return capabilities;
    }

    public String getContact() {
        return contact;
    }

    public String getCategory() {
        return category;
    }

    public String getCertifications() {
        return certifications;
    }

    public GeoPoint getLocation() {
        return location;
    }
}
