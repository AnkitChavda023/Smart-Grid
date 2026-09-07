package com.smartgrid.mcpserver.tool;

import com.smartgrid.mcpserver.config.DownstreamClients;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * No "procurement manager" role or audience exists anywhere in this system's data model (auth-service's
 * roles are SUPPLIER/PLANNER/ADMIN) — this calls notification-service's generic ad-hoc fan-out, the
 * same documented simplification used for every notification path with no real per-role targeting yet.
 */
@Component
public class NotifyProcurementManagerTool implements McpTool {

    private final DownstreamClients clients;

    public NotifyProcurementManagerTool(DownstreamClients clients) {
        this.clients = clients;
    }

    @Override
    public String name() {
        return "notifyProcurementManager";
    }

    @Override
    public int version() {
        return 1;
    }

    @Override
    public String description() {
        return "Sends a notification about a predicted SLA breach risk that may need procurement attention.";
    }

    @Override
    public ToolSchema inputSchema() {
        return ToolSchema.object(Map.of(
                        "title", new ToolSchema.PropertySchema("string", "Notification title"),
                        "body", new ToolSchema.PropertySchema("string", "Notification body"),
                        "relatedEntityId", new ToolSchema.PropertySchema("string", "Related vendor/order/contract id, if any")),
                List.of("title", "body"));
    }

    @Override
    public ToolSchema outputSchema() {
        return ToolSchema.object(Map.of(
                        "sent", new ToolSchema.PropertySchema("boolean", "Whether the notification call succeeded")),
                List.of());
    }

    @Override
    public Object invoke(Map<String, Object> params) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("title", ToolParams.requireString(params, "title"));
        body.put("body", ToolParams.requireString(params, "body"));
        body.put("relatedEntityId", ToolParams.optionalString(params, "relatedEntityId"));

        clients.notificationService().post()
                .uri("/notifications/adhoc")
                .body(body)
                .retrieve()
                .toBodilessEntity();

        return Map.of("sent", true);
    }
}
