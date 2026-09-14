package com.smartgrid.mcpserver.tool;

import com.smartgrid.mcpserver.config.DownstreamClients;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Same real mutation as a manual restock — inventory-service's {@code POST /inventory/replenish} —
 * called under a name that reflects who/why triggered it (a predicted-risk agent, not a purchasing
 * decision by a human). No separate "preemptive" endpoint exists or is needed; the inventory ledger
 * doesn't distinguish reasons for a replenishment, only that one happened.
 */
@Component
public class TriggerPreemptiveRestockTool implements McpTool {

    private final DownstreamClients clients;

    public TriggerPreemptiveRestockTool(DownstreamClients clients) {
        this.clients = clients;
    }

    @Override
    public String name() {
        return "triggerPreemptiveRestock";
    }

    @Override
    public int version() {
        return 1;
    }

    @Override
    public String description() {
        return "Triggers an inventory replenishment ahead of a predicted stock-out or SLA breach.";
    }

    @Override
    public ToolSchema inputSchema() {
        return ToolSchema.object(Map.of(
                        "skuId", new ToolSchema.PropertySchema("string", "SKU identifier"),
                        "warehouseId", new ToolSchema.PropertySchema("string", "Warehouse identifier"),
                        "quantity", new ToolSchema.PropertySchema("integer", "Quantity to replenish")),
                List.of("skuId", "warehouseId", "quantity"));
    }

    @Override
    public ToolSchema outputSchema() {
        return ToolSchema.object(Map.of(
                        "triggered", new ToolSchema.PropertySchema("boolean", "Whether the replenishment call succeeded")),
                List.of());
    }

    @Override
    public Object invoke(Map<String, Object> params) {
        Map<String, Object> body = Map.of(
                "skuId", ToolParams.requireString(params, "skuId"),
                "warehouseId", ToolParams.requireString(params, "warehouseId"),
                "quantity", ToolParams.requireInt(params, "quantity"));

        clients.inventoryService().post()
                .uri("/inventory/replenish")
                .body(body)
                .retrieve()
                .toBodilessEntity();

        return Map.of("triggered", true);
    }
}
