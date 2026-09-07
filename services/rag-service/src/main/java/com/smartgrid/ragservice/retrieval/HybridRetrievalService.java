package com.smartgrid.ragservice.retrieval;

import com.smartgrid.ragservice.config.RagProperties;
import com.smartgrid.ragservice.domain.DocumentChunk;
import com.smartgrid.ragservice.embedding.OpenAiEmbeddingClient;
import com.smartgrid.ragservice.domain.SourceType;
import com.smartgrid.ragservice.repository.DocumentChunkRepository;
import com.smartgrid.ragservice.search.KeywordSearchService;
import com.smartgrid.ragservice.vector.VectorHit;
import com.smartgrid.ragservice.vector.VectorStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reciprocal Rank Fusion: combines two ranked lists by rank position, not raw score, so pgvector's
 * cosine distance and Elasticsearch's BM25 score never need to be normalized onto a shared scale
 * (they are not comparable quantities, and weighted-sum fusion would require constant retuning).
 */
@Service
public class HybridRetrievalService {

    private final OpenAiEmbeddingClient embeddingClient;
    private final VectorStore vectorStore;
    private final KeywordSearchService keywordSearchService;
    private final DocumentChunkRepository chunkRepository;
    private final RagProperties properties;

    public HybridRetrievalService(OpenAiEmbeddingClient embeddingClient, VectorStore vectorStore,
                                   KeywordSearchService keywordSearchService, DocumentChunkRepository chunkRepository,
                                   RagProperties properties) {
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
        this.keywordSearchService = keywordSearchService;
        this.chunkRepository = chunkRepository;
        this.properties = properties;
    }

    public List<RetrievedChunk> search(String query, int k) {
        float[] queryEmbedding = embeddingClient.embedSingle(query);
        List<VectorHit> vectorHits = vectorStore.similaritySearch(queryEmbedding, properties.retrieval().vectorCandidates());
        List<UUID> keywordHits = keywordSearchService.search(query, properties.retrieval().keywordCandidates());

        int rrfK = properties.retrieval().rrfK();
        Map<UUID, Double> fusedScores = new HashMap<>();
        for (int rank = 0; rank < vectorHits.size(); rank++) {
            UUID id = vectorHits.get(rank).chunkId();
            fusedScores.merge(id, 1.0 / (rrfK + rank + 1), Double::sum);
        }
        for (int rank = 0; rank < keywordHits.size(); rank++) {
            UUID id = keywordHits.get(rank);
            fusedScores.merge(id, 1.0 / (rrfK + rank + 1), Double::sum);
        }

        return fusedScores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(k)
                .map(entry -> chunkRepository.findById(entry.getKey())
                        .map(chunk -> toRetrievedChunk(chunk, entry.getValue()))
                        .orElse(null))
                .filter(chunk -> chunk != null)
                .toList();
    }

    private RetrievedChunk toRetrievedChunk(DocumentChunk chunk, double fusedScore) {
        return new RetrievedChunk(chunk.getId(), chunk.getSourceType(), chunk.getSourceId(), chunk.getContent(), chunk.getVendorId(), fusedScore);
    }

    public record RetrievedChunk(UUID chunkId, SourceType sourceType,
                                  String sourceId, String content, String vendorId, double score) {
    }
}
