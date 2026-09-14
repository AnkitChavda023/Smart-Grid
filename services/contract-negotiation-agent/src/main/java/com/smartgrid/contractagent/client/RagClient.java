package com.smartgrid.contractagent.client;

import com.smartgrid.contractagent.config.ContractNegotiationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/** Draws on whatever M14 has indexed for this vendor — past contract terms, breach reasons — as the real approximation of "past contracts + industry benchmarks." */
@Component
public class RagClient {

    private final RestClient restClient;

    public RagClient(ContractNegotiationProperties properties) {
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
