package com.smartgrid.mcpserver.tool;

import com.smartgrid.mcpserver.config.DownstreamClients;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MCP tool for searching and ranking vendors by SKU, destination region, and minimum reliability.
 */
@Component
public class SearchVendorsToolV2 implements McpTool {

    private final DownstreamClients clients;

    public SearchVendorsToolV2(DownstreamClients clients) {
        this.clients = clients;
    }

    @Override
    public String name() {
        return "searchVendors";
    }

    @Override
    public int version() {
        return 2;
    }

    @Override
    public String description() {
        return "Ranks vendors that carry a given SKU by composite score, with working region filtering and a minimum reliability threshold.";
    }

    @Override
    public ToolSchema inputSchema() {
        return ToolSchema.object(Map.of(
                        "sku", new ToolSchema.PropertySchema("string", "SKU identifier to rank vendors for"),
                        "region", new ToolSchema.PropertySchema("string", "Restrict results to vendors in this region"),
                        "maxLeadTime", new ToolSchema.PropertySchema("integer", "Maximum acceptable lead time in days"),
                        "minReliability", new ToolSchema.PropertySchema("number", "Minimum vendor reliability score, 0-1")),
                List.of("sku"));
    }

    @Override
    public ToolSchema outputSchema() {
        return ToolSchema.object(Map.of(
                        "vendorId", new ToolSchema.PropertySchema("string", "Vendor id"),
                        "vendorName", new ToolSchema.PropertySchema("string", "Vendor name"),
                        "region", new ToolSchema.PropertySchema("string", "Vendor region"),
                        "compositeScore", new ToolSchema.PropertySchema("number", "Ranking score"),
                        "price", new ToolSchema.PropertySchema("number", "Unit price"),
                        "leadTimeDays", new ToolSchema.PropertySchema("integer", "Lead time in days"),
                        "reliabilityScore", new ToolSchema.PropertySchema("number", "Vendor reliability score")),
                List.of());
    }

    @Override
    public Object invoke(Map<String, Object> params) {
        String sku = ToolParams.requireString(params, "sku");
        String region = ToolParams.optionalString(params, "region");
        int maxLeadTime = ToolParams.optionalInt(params, "maxLeadTime", Integer.MAX_VALUE);
        double minReliability = ToolParams.optionalDouble(params, "minReliability", 0.0);

        VendorRankingResultDto[] ranked = clients.vendorService().get()
                .uri("/vendors/top?sku={sku}&k=50", sku)
                .retrieve()
                .body(VendorRankingResultDto[].class);

        VendorCatalogHitDto[] catalog = clients.vendorService().get()
                .uri("/vendors/search")
                .retrieve()
                .body(VendorCatalogHitDto[].class);
        Map<String, String> regionByVendorId = catalog == null ? Map.of() :
                java.util.Arrays.stream(catalog).collect(Collectors.toMap(VendorCatalogHitDto::id, VendorCatalogHitDto::region, (a, b) -> a));

        Set<String> allowedIds = region == null ? null : regionByVendorId.entrySet().stream()
                .filter(e -> region.equalsIgnoreCase(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        return (ranked == null ? List.<VendorRankingResultDto>of() : List.of(ranked)).stream()
                .filter(v -> v.leadTimeDays() <= maxLeadTime)
                .filter(v -> v.reliabilityScore() >= minReliability)
                .filter(v -> allowedIds == null || allowedIds.contains(v.vendorId()))
                .map(v -> new EnrichedResult(v.vendorId(), v.vendorName(), regionByVendorId.get(v.vendorId()),
                        v.compositeScore(), v.price(), v.leadTimeDays(), v.reliabilityScore()))
                .toList();
    }

    private record EnrichedResult(String vendorId, String vendorName, String region, double compositeScore,
                                   double price, int leadTimeDays, double reliabilityScore) {
    }
}
