package com.smartgrid.ragservice.embedding;

import com.smartgrid.ragservice.config.RagProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Comparator;
import java.util.List;

@Component
public class OpenAiEmbeddingClient {

    private final RestClient restClient;
    private final RagProperties.OpenAi config;

    public OpenAiEmbeddingClient(RagProperties properties) {
        this.config = properties.openai();
        this.restClient = RestClient.builder()
                .baseUrl(config.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.apiKey())
                .build();
    }

    public List<float[]> embed(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }
        EmbeddingResponse response = restClient.post()
                .uri("/embeddings")
                .body(new EmbeddingRequest(config.embeddingModel(), texts))
                .retrieve()
                .body(EmbeddingResponse.class);

        return response.data().stream()
                .sorted(Comparator.comparingInt(EmbeddingData::index))
                .map(d -> {
                    float[] vector = new float[d.embedding().size()];
                    for (int i = 0; i < vector.length; i++) {
                        vector[i] = d.embedding().get(i).floatValue();
                    }
                    return vector;
                })
                .toList();
    }

    public float[] embedSingle(String text) {
        return embed(List.of(text)).get(0);
    }

    private record EmbeddingRequest(String model, List<String> input) {
    }

    private record EmbeddingResponse(List<EmbeddingData> data) {
    }

    private record EmbeddingData(int index, List<Double> embedding) {
    }
}
