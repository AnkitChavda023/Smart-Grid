package com.smartgrid.mcpserver.tool;

import com.smartgrid.mcpserver.config.DownstreamClients;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The module's hard rule lives on the contract-service side, not here: this tool can only ever
 * reach {@code POST /contracts/drafts} (creates a DRAFT-status row). There is deliberately no
 * "submitContract" tool anywhere in this server — {@code POST /contracts/drafts/{id}/submit} is
 * reachable only by a real PLANNER/ADMIN JWT calling contract-service directly, never through MCP.
 */
@Component
public class SaveDraftContractTool implements McpTool {

    private final DownstreamClients clients;

    public SaveDraftContractTool(DownstreamClients clients) {
        this.clients = clients;
    }

    @Override
    public String name() {
        return "saveDraftContract";
    }

    @Override
    public int version() {
        return 1;
    }

    @Override
    public String description() {
        return "Saves a proposed contract draft for human review. Does not activate a contract — only a PLANNER/ADMIN can submit a draft.";
    }

    @Override
    public ToolSchema inputSchema() {
        return ToolSchema.object(Map.of(
                        "vendorId", new ToolSchema.PropertySchema("string", "Vendor id"),
                        "existingContractId", new ToolSchema.PropertySchema("string", "Existing contract id being renewed, if any"),
                        "proposedTerms", new ToolSchema.PropertySchema("string", "Proposed contract terms text"),
                        "summary", new ToolSchema.PropertySchema("string", "Summary of what changed and why")),
                List.of("vendorId", "proposedTerms", "summary"));
    }

    @Override
    public ToolSchema outputSchema() {
        return ToolSchema.object(Map.of(
                        "id", new ToolSchema.PropertySchema("string", "Created draft id"),
                        "status", new ToolSchema.PropertySchema("string", "Draft status")),
                List.of());
    }

    @Override
    public Object invoke(Map<String, Object> params) {
        Map<String, Object> body = new HashMap<>();
        body.put("vendorId", ToolParams.requireString(params, "vendorId"));
        body.put("existingContractId", ToolParams.optionalString(params, "existingContractId"));
        body.put("proposedTerms", ToolParams.requireString(params, "proposedTerms"));
        body.put("summary", ToolParams.requireString(params, "summary"));

        return clients.contractService().post()
                .uri("/contracts/drafts")
                .body(body)
                .retrieve()
                .body(Object.class);
    }
}
