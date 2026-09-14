package com.smartgrid.ragservice.repository;

import com.smartgrid.ragservice.domain.DocumentChunk;
import com.smartgrid.ragservice.domain.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {
    List<DocumentChunk> findBySourceTypeAndSourceId(SourceType sourceType, String sourceId);

    void deleteBySourceTypeAndSourceId(SourceType sourceType, String sourceId);

    long countBySourceType(SourceType sourceType);
}
