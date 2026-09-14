package com.smartgrid.mcpserver.tool;

import com.smartgrid.mcpserver.config.DownstreamClients;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetActiveContractTool implements McpTool {

    private final DownstreamClients clients;

    public GetActiveContractTool(DownstreamClients clients) {
        this.clients = clients;
    }

    @Override
    public String name() {
        return "getActiveContract";
    }

    @Override
    public int version() {
        return 1;
    }

    @Override
    public String description() {
        return "Returns the active contract(s) currently on file for a vendor.";
    }

    @Override
    public ToolSchema inputSchema() {
        return ToolSchema.object(Map.of(
                        "vendorId", new ToolSchema.PropertySchema("string", "Vendor id")),
                List.of("vendorId"));
    }

    @Override
    public ToolSchema outputSchema() {
        return ToolSchema.object(Map.of(
                        "id", new ToolSchema.PropertySchema("string", "Contract id"),
                        "terms", new ToolSchema.PropertySchema("string", "Contract terms"),
                        "startDate", new ToolSchema.PropertySchema("string", "Start date"),
                        "endDate", new ToolSchema.PropertySchema("string", "End date")),
                List.of());
    }

    @Override
    public Object invoke(Map<String, Object> params) {
        String vendorId = ToolParams.requireString(params, "vendorId");

        ContractDto[] contracts = clients.contractService().get()
                .uri("/contracts/{vendorId}/active", vendorId)
                .retrieve()
                .body(ContractDto[].class);

        return contracts == null ? List.of() : List.of(contracts);
    }

    private record ContractDto(UUID id, String vendorId, String terms, LocalDate startDate, LocalDate endDate,
                                boolean active, List<Object> slaTerms) {
    }
}
