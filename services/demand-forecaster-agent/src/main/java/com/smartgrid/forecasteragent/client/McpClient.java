package com.smartgrid.forecasteragent.client;

import com.smartgrid.forecasteragent.config.DemandForecasterProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class McpClient {

    private final RestClient restClient;

    public McpClient(DemandForecasterProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.mcpServer().baseUrl())
                .defaultHeader("X-Internal-Token", properties.mcpServer().internalToken())
                .build();
    }

    public <T> T call(String toolName, int version, Map<String, Object> params, Class<T> responseType) {
        return restClient.post()
                .uri("/v{version}/tools/{name}", version, toolName)
                .body(params)
                .retrieve()
                .body(responseType);
    }
}
