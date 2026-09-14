package com.smartgrid.ragservice.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface RagChunkDocumentRepository extends ElasticsearchRepository<RagChunkDocument, String> {
    void deleteBySourceTypeAndSourceId(String sourceType, String sourceId);
}
