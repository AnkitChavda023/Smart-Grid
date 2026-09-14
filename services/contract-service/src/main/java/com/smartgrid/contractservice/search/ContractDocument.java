package com.smartgrid.contractservice.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "contracts_index")
public class ContractDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String vendorId;

    @Field(type = FieldType.Text)
    private String terms;

    @Field(type = FieldType.Keyword)
    private String startDate;

    @Field(type = FieldType.Keyword)
    private String endDate;

    @Field(type = FieldType.Boolean)
    private boolean active;

    protected ContractDocument() {
    }

    public ContractDocument(String id, String vendorId, String terms, String startDate, String endDate, boolean active) {
        this.id = id;
        this.vendorId = vendorId;
        this.terms = terms;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public String getVendorId() {
        return vendorId;
    }

    public String getTerms() {
        return terms;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public boolean isActive() {
        return active;
    }
}
