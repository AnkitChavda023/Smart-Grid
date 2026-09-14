package com.smartgrid.mcpserver.tool;

import com.smartgrid.mcpserver.config.DownstreamClients;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MCP tool for estimating vendor lead time based on catalog averages.
 */
@Component
public class EstimateLeadTimeTool implements McpTool {

    private final DownstreamClients clients;

    public EstimateLeadTimeTool(DownstreamClients clients) {
        this.clients = clients;
    }

    @Override
    public String name() {
        return "estimateLeadTime";
    }

    @Override
    public int version() {
        return 1;
    }

    @Override
    public String description() {
        return "Estimates a vendor's lead time by averaging leadTimeDays across that vendor's real SKU catalog.";
    }

    @Override
    public ToolSchema inputSchema() {
        return ToolSchema.object(Map.of(
                        "vendorId", new ToolSchema.PropertySchema("string", "Vendor id"),
                        "destination", new ToolSchema.PropertySchema("string", "Not applied — no destination-aware logistics model exists yet")),
                List.of("vendorId"));
    }

    @Override
    public ToolSchema outputSchema() {
        return ToolSchema.object(Map.of(
                        "vendorId", new ToolSchema.PropertySchema("string", "Vendor id"),
                        "averageLeadTimeDays", new ToolSchema.PropertySchema("number", "Average lead time across the vendor's SKU catalog"),
                        "skuCount", new ToolSchema.PropertySchema("integer", "Number of SKUs the average was computed over")),
                List.of());
    }

    @Override
    public Object invoke(Map<String, Object> params) {
        String vendorId = ToolParams.requireString(params, "vendorId");

        VendorSkuDto[] skus = clients.vendorService().get()
                .uri("/vendors/{id}/skus", vendorId)
                .retrieve()
                .body(VendorSkuDto[].class);

        List<VendorSkuDto> list = skus == null ? List.of() : List.of(skus);
        double average = list.stream().mapToInt(VendorSkuDto::leadTimeDays).average().orElse(0);
        return new EstimateResult(vendorId, average, list.size());
    }

    private record VendorSkuDto(String skuId, double price, int leadTimeDays) {
    }

    private record EstimateResult(String vendorId, double averageLeadTimeDays, int skuCount) {
    }
}
