package com.smartgrid.mcpserver.tool;

public record ToolDefinition(String name, int version, String description, ToolSchema inputSchema, ToolSchema outputSchema) {

    public static ToolDefinition of(McpTool tool) {
        return new ToolDefinition(tool.name(), tool.version(), tool.description(), tool.inputSchema(), tool.outputSchema());
    }
}
