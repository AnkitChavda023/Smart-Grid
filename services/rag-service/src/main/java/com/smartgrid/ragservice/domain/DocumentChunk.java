package com.smartgrid.ragservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * The {@code embedding} column is deliberately not mapped here — it is written and read via
 * raw JDBC by {@link com.smartgrid.ragservice.vector.VectorStore}, since JPQL has no operator
 * for pgvector's {@code <=>} cosine-distance search and Hibernate has no in-BOM vector type
 * for this project's pinned ORM version. This entity owns everything else about a chunk.
 */
@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    @Column(name = "source_id", nullable = false)
    private String sourceId;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(nullable = false)
    private String content;

    @Column(name = "vendor_id")
    private String vendorId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DocumentChunk() {
    }

    public DocumentChunk(UUID id, SourceType sourceType, String sourceId, int chunkIndex, String content, String vendorId) {
        this.id = id;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.vendorId = vendorId;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public String getVendorId() {
        return vendorId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
