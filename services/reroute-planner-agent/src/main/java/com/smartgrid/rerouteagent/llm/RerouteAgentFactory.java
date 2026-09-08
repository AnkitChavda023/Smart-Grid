package com.smartgrid.rerouteagent.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgrid.commons.metrics.AgentMetricsRecorder;
import com.smartgrid.rerouteagent.client.McpClient;
import com.smartgrid.rerouteagent.config.RerouteProperties;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.stereotype.Component;

/** Builds a fresh {@link RerouteAgent} + {@link RerouteTools} pair per reroute attempt — see RerouteTools's own note on why this state isn't a shared singleton. */
@Component
public class RerouteAgentFactory {

    private final ChatModel chatModel;
    private final McpClient mcpClient;
    private final ObjectMapper objectMapper;
    private final RerouteProperties properties;
    private final AgentMetricsRecorder metrics;

    public RerouteAgentFactory(ChatModel chatModel, McpClient mcpClient, ObjectMapper objectMapper,
                                RerouteProperties properties, AgentMetricsRecorder metrics) {
        this.chatModel = chatModel;
        this.mcpClient = mcpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.metrics = metrics;
    }

    public RerouteSession newSession() {
        RerouteTools tools = new RerouteTools(mcpClient, objectMapper, properties.maxToolCalls(), metrics);
        RerouteAgent agent = AiServices.builder(RerouteAgent.class)
                .chatModel(chatModel)
                .tools(tools)
                .maxSequentialToolsInvocations(properties.maxToolCalls())
                .build();
        return new RerouteSession(agent, tools);
    }

    public record RerouteSession(RerouteAgent agent, RerouteTools tools) {
    }
}
