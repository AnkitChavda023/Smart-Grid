package com.smartgrid.ragservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartgrid.rag")
public record RagProperties(
        OpenAi openai,
        Chunk chunk,
        Retrieval retrieval,
        ServiceEndpoint vendorService,
        ServiceEndpoint contractService
) {
    public record OpenAi(String apiKey, String baseUrl, String embeddingModel, int embeddingDimensions) {
    }

    public record Chunk(int maxChars, int overlapChars) {
    }

    public record Retrieval(int vectorCandidates, int keywordCandidates, int rrfK) {
    }

    public record ServiceEndpoint(String baseUrl) {
    }
}
