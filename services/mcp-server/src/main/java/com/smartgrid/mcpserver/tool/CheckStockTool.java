package com.smartgrid.mcpserver.tool;

import com.smartgrid.mcpserver.config.DownstreamClients;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * inventory-service tracks availability per SKU only, not per vendor — {@code vendorId} is accepted
 * for schema fidelity with the module spec but not applied to the downstream call.
 */
@Component
public class CheckStockTool implements McpTool {

    private final DownstreamClients clients;

    public CheckStockTool(DownstreamClients clients) {
        this.clients = clients;
    }

    @Override
    public String name() {
        return "checkStock";
    }

    @Override
    public int version() {
        return 1;
    }

    @Override
    public String description() {
        return "Checks whether a SKU has enough available inventory for a requested quantity.";
    }

    @Override
    public ToolSchema inputSchema() {
        return ToolSchema.object(Map.of(
                        "vendorId", new ToolSchema.PropertySchema("string", "Not applied — inventory is tracked per-SKU, not per-vendor"),
                        "skuId", new ToolSchema.PropertySchema("string", "SKU identifier"),
                        "quantity", new ToolSchema.PropertySchema("integer", "Requested quantity")),
                List.of("skuId", "quantity"));
    }

    @Override
    public ToolSchema outputSchema() {
        return ToolSchema.object(Map.of(
                        "skuId", new ToolSchema.PropertySchema("string", "SKU identifier"),
                        "availableQuantity", new ToolSchema.PropertySchema("integer", "Currently available quantity"),
                        "sufficient", new ToolSchema.PropertySchema("boolean", "Whether availableQuantity meets the requested quantity")),
                List.of());
    }

    @Override
    public Object invoke(Map<String, Object> params) {
        String skuId = ToolParams.requireString(params, "skuId");
        int quantity = ToolParams.requireInt(params, "quantity");

        AvailabilityDto availability = clients.inventoryService().get()
                .uri("/inventory/{sku}/available", skuId)
                .retrieve()
                .body(AvailabilityDto.class);

        long available = availability == null ? 0 : availability.availableQuantity();
        return new CheckStockResult(skuId, available, available >= quantity);
    }

    private record AvailabilityDto(String skuId, long availableQuantity) {
    }

    private record CheckStockResult(String skuId, long availableQuantity, boolean sufficient) {
    }
}
