package com.smartgrid.forecasteragent.client;

import com.smartgrid.forecasteragent.config.DemandForecasterProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/** REST client for retrieving contextual information from the RAG service. */
@Component
public class RagClient {

    private final RestClient restClient;

    public RagClient(DemandForecasterProperties properties) {
        this.restClient = RestClient.create(properties.ragService().baseUrl());
    }

    public List<String> search(String query, int k) {
        RagHit[] hits = restClient.post()
                .uri("/rag/search")
                .body(new SearchRequest(query, k))
                .retrieve()
                .body(RagHit[].class);
        return hits == null ? List.of() : java.util.Arrays.stream(hits).map(RagHit::content).toList();
    }

    private record SearchRequest(String query, int k) {
    }

    private record RagHit(String content) {
    }
}
