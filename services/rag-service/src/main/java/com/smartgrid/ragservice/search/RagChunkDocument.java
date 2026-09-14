package com.smartgrid.ragservice.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "rag-chunks")
public class RagChunkDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String sourceType;

    @Field(type = FieldType.Keyword)
    private String sourceId;

    @Field(type = FieldType.Text)
    private String content;

    @Field(type = FieldType.Keyword)
    private String vendorId;

    protected RagChunkDocument() {
    }

    public RagChunkDocument(String id, String sourceType, String sourceId, String content, String vendorId) {
        this.id = id;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.content = content;
        this.vendorId = vendorId;
    }

    public String getId() {
        return id;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getContent() {
        return content;
    }

    public String getVendorId() {
        return vendorId;
    }
}
