package com.smartgrid.mcpserver.tool;

import com.smartgrid.mcpserver.config.DownstreamClients;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * v1: ranks vendors for a SKU via vendor-service's {@code /vendors/top}. {@code region} is accepted
 * for schema fidelity with the module spec but not applied — {@code VendorRankingResult} carries no
 * region field, only {@code /vendors/search} (a different, non-ranked index) does. See v2.
 */
@Component
public class SearchVendorsToolV1 implements McpTool {

    private final DownstreamClients clients;

    public SearchVendorsToolV1(DownstreamClients clients) {
        this.clients = clients;
    }

    @Override
    public String name() {
        return "searchVendors";
    }

    @Override
    public int version() {
        return 1;
    }

    @Override
    public String description() {
        return "Ranks vendors that carry a given SKU by composite score (price, lead time, reliability).";
    }

    @Override
    public ToolSchema inputSchema() {
        return ToolSchema.object(Map.of(
                        "sku", new ToolSchema.PropertySchema("string", "SKU identifier to rank vendors for"),
                        "region", new ToolSchema.PropertySchema("string", "Not applied in v1 — accepted for schema compatibility only"),
                        "maxLeadTime", new ToolSchema.PropertySchema("integer", "Maximum acceptable lead time in days")),
                List.of("sku"));
    }

    @Override
    public ToolSchema outputSchema() {
        return ToolSchema.object(Map.of(
                        "vendorId", new ToolSchema.PropertySchema("string", "Vendor id"),
                        "vendorName", new ToolSchema.PropertySchema("string", "Vendor name"),
                        "compositeScore", new ToolSchema.PropertySchema("number", "Ranking score"),
                        "price", new ToolSchema.PropertySchema("number", "Unit price"),
                        "leadTimeDays", new ToolSchema.PropertySchema("integer", "Lead time in days"),
                        "reliabilityScore", new ToolSchema.PropertySchema("number", "Vendor reliability score")),
                List.of());
    }

    @Override
    public Object invoke(Map<String, Object> params) {
        String sku = ToolParams.requireString(params, "sku");
        int maxLeadTime = ToolParams.optionalInt(params, "maxLeadTime", Integer.MAX_VALUE);

        VendorRankingResultDto[] ranked = clients.vendorService().get()
                .uri("/vendors/top?sku={sku}&k=50", sku)
                .retrieve()
                .body(VendorRankingResultDto[].class);

        return (ranked == null ? List.<VendorRankingResultDto>of() : List.of(ranked)).stream()
                .filter(v -> v.leadTimeDays() <= maxLeadTime)
                .toList();
    }
}
