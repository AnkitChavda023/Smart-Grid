package com.smartgrid.rerouteagent.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgrid.commons.metrics.AgentMetricsRecorder;
import com.smartgrid.rerouteagent.client.McpClient;
import com.smartgrid.rerouteagent.domain.DagTraceNode;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One instance per reroute attempt (not a Spring singleton) — its trace list and call counter are
 * request-scoped state, and a fresh instance per attempt is simpler and safer than making that
 * state thread-safe for concurrent reroutes. Each @Tool method really does call the real MCP
 * server; "llmReasoning" on the resulting trace node is a short mechanical label (which step this
 * was), not the model's literal token-level reasoning — LangChain4j's AiServices abstraction
 * doesn't expose per-tool-call reasoning text without dropping to manual ChatModel orchestration,
 * and inventing plausible-looking "reasoning" text here would be worse than being honest about it.
 */
public class RerouteTools {

    private final McpClient mcpClient;
    private final ObjectMapper objectMapper;
    private final int maxCalls;
    private final AgentMetricsRecorder metrics;
    private final List<DagTraceNode> trace = new ArrayList<>();

    public RerouteTools(McpClient mcpClient, ObjectMapper objectMapper, int maxCalls, AgentMetricsRecorder metrics) {
        this.mcpClient = mcpClient;
        this.objectMapper = objectMapper;
        this.maxCalls = maxCalls;
        this.metrics = metrics;
    }

    @Tool("Search and rank vendors that carry a given SKU, optionally filtered by region and a minimum reliability score")
    public String searchVendors(@P("SKU identifier") String sku,
                                 @P(value = "Region to restrict results to", required = false) String region,
                                 @P(value = "Minimum reliability score, 0 to 1", required = false) Double minReliability) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("sku", sku);
        if (region != null) params.put("region", region);
        if (minReliability != null) params.put("minReliability", minReliability);
        return invoke("searchVendors", 2, params);
    }

    @Tool("Check whether a SKU has enough available inventory for a requested quantity")
    public String checkStock(@P("SKU identifier") String skuId, @P("Requested quantity") int quantity) {
        return invoke("checkStock", 1, Map.of("skuId", skuId, "quantity", quantity));
    }

    @Tool("Create a price quote from a vendor for a SKU and quantity, against a specific order")
    public String createQuote(@P("Order id this quote is for") String orderId, @P("Vendor id") String vendorId,
                               @P("SKU identifier") String skuId, @P("Requested quantity") int quantity) {
        return invoke("createQuote", 1, Map.of("orderId", orderId, "vendorId", vendorId, "skuId", skuId, "quantity", quantity));
    }

    @Tool("Accept a previously created quote, finalizing the reroute")
    public String acceptQuote(@P("Quote id to accept") String quoteId) {
        return invoke("acceptQuote", 1, Map.of("quoteId", quoteId));
    }

    private String invoke(String toolName, int version, Map<String, Object> params) {
        if (trace.size() >= maxCalls) {
            throw new IllegalStateException("Max tool call depth (" + maxCalls + ") exceeded");
        }
        metrics.recordToolCall(toolName);
        long start = System.nanoTime();
        String outputJson;
        try {
            Object result = mcpClient.call(toolName, version, params, Object.class);
            outputJson = toJson(result);
        } catch (Exception e) {
            outputJson = "{\"error\":" + toJson(e.getMessage()) + "}";
        }
        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        int stepIndex = trace.size();
        trace.add(new DagTraceNode(stepIndex, toolName, toJson(params), outputJson, latencyMs,
                "ReAct step " + (stepIndex + 1) + ": invoke " + toolName));
        return outputJson;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    public List<DagTraceNode> getTrace() {
        return trace;
    }
}
