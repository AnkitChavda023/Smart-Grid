package com.smartgrid.mcpserver.tool;

import com.smartgrid.mcpserver.config.DownstreamClients;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class UpdateSafetyStockLevelTool implements McpTool {

    private final DownstreamClients clients;

    public UpdateSafetyStockLevelTool(DownstreamClients clients) {
        this.clients = clients;
    }

    @Override
    public String name() {
        return "updateSafetyStockLevel";
    }

    @Override
    public int version() {
        return 1;
    }

    @Override
    public String description() {
        return "Sets the safety stock (reorder threshold) level for a SKU at a warehouse.";
    }

    @Override
    public ToolSchema inputSchema() {
        return ToolSchema.object(Map.of(
                        "skuId", new ToolSchema.PropertySchema("string", "SKU identifier"),
                        "warehouseId", new ToolSchema.PropertySchema("string", "Warehouse identifier"),
                        "safetyStockLevel", new ToolSchema.PropertySchema("integer", "New safety stock level")),
                List.of("skuId", "warehouseId", "safetyStockLevel"));
    }

    @Override
    public ToolSchema outputSchema() {
        return ToolSchema.object(Map.of(
                        "updated", new ToolSchema.PropertySchema("boolean", "Whether the update succeeded")),
                List.of());
    }

    @Override
    public Object invoke(Map<String, Object> params) {
        String skuId = ToolParams.requireString(params, "skuId");
        String warehouseId = ToolParams.requireString(params, "warehouseId");
        int level = ToolParams.requireInt(params, "safetyStockLevel");

        clients.inventoryService().put()
                .uri("/inventory/{sku}/{warehouseId}/safety-stock", skuId, warehouseId)
                .body(Map.of("safetyStockLevel", level))
                .retrieve()
                .toBodilessEntity();

        return Map.of("updated", true);
    }
}
