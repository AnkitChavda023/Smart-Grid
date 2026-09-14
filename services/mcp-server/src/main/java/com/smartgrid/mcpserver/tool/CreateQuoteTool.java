package com.smartgrid.mcpserver.tool;

import com.smartgrid.mcpserver.config.DownstreamClients;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MCP tool for creating quotes for orders via pricing-service.
 */
@Component
public class CreateQuoteTool implements McpTool {

    private final DownstreamClients clients;

    public CreateQuoteTool(DownstreamClients clients) {
        this.clients = clients;
    }

    @Override
    public String name() {
        return "createQuote";
    }

    @Override
    public int version() {
        return 1;
    }

    @Override
    public String description() {
        return "Creates a price quote from a vendor for a SKU and quantity against a real order.";
    }

    @Override
    public ToolSchema inputSchema() {
        return ToolSchema.object(Map.of(
                        "orderId", new ToolSchema.PropertySchema("string", "Order this quote is being requested for (required by pricing-service)"),
                        "vendorId", new ToolSchema.PropertySchema("string", "Vendor id"),
                        "skuId", new ToolSchema.PropertySchema("string", "SKU identifier"),
                        "quantity", new ToolSchema.PropertySchema("integer", "Requested quantity")),
                List.of("orderId", "vendorId", "skuId", "quantity"));
    }

    @Override
    public ToolSchema outputSchema() {
        return ToolSchema.object(Map.of(
                        "id", new ToolSchema.PropertySchema("string", "Created quote id"),
                        "status", new ToolSchema.PropertySchema("string", "Quote status"),
                        "totalPrice", new ToolSchema.PropertySchema("number", "Total quoted price"),
                        "expiresAt", new ToolSchema.PropertySchema("string", "Quote expiry timestamp")),
                List.of());
    }

    @Override
    public Object invoke(Map<String, Object> params) {
        String orderId = ToolParams.requireString(params, "orderId");
        String vendorId = ToolParams.requireString(params, "vendorId");
        String skuId = ToolParams.requireString(params, "skuId");
        int quantity = ToolParams.requireInt(params, "quantity");

        QuoteRequestBody body = new QuoteRequestBody(orderId, vendorId, List.of(new QuoteItem(skuId, quantity)));
        QuoteResponseDto response = clients.pricingService().post()
                .uri("/quotes")
                .body(body)
                .retrieve()
                .body(QuoteResponseDto.class);

        return response;
    }

    private record QuoteItem(String skuId, int quantity) {
    }

    private record QuoteRequestBody(String orderId, String vendorId, List<QuoteItem> items) {
    }

    private record QuoteResponseDto(UUID id, String orderId, String vendorId, String status, double totalPrice,
                                     List<Object> items, Instant createdAt, Instant expiresAt) {
    }
}
