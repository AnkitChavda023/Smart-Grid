package com.smartgrid.ragservice.search;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class KeywordSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public KeywordSearchService(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    /** BM25 match on chunk content, ranked by Elasticsearch's default relevance score. */
    public List<UUID> search(String query, int k) {
        CriteriaQuery criteriaQuery = new CriteriaQuery(Criteria.where("content").matches(query));
        criteriaQuery.setMaxResults(k);
        return elasticsearchOperations.search(criteriaQuery, RagChunkDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .map(RagChunkDocument::getId)
                .map(UUID::fromString)
                .toList();
    }
}
