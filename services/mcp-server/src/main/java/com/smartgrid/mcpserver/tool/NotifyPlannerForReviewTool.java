package com.smartgrid.mcpserver.tool;

import com.smartgrid.mcpserver.config.DownstreamClients;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Same generic ad-hoc notification path as notifyProcurementManager/alertPlannerForReorder — see those tools' notes on why. */
@Component
public class NotifyPlannerForReviewTool implements McpTool {

    private final DownstreamClients clients;

    public NotifyPlannerForReviewTool(DownstreamClients clients) {
        this.clients = clients;
    }

    @Override
    public String name() {
        return "notifyPlannerForReview";
    }

    @Override
    public int version() {
        return 1;
    }

    @Override
    public String description() {
        return "Sends a notification that a contract draft is ready for PLANNER/ADMIN review and submission.";
    }

    @Override
    public ToolSchema inputSchema() {
        return ToolSchema.object(Map.of(
                        "title", new ToolSchema.PropertySchema("string", "Notification title"),
                        "body", new ToolSchema.PropertySchema("string", "Notification body"),
                        "relatedEntityId", new ToolSchema.PropertySchema("string", "Related draft id, if any")),
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
        Map<String, Object> body = new HashMap<>();
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
