package com.smartgrid.mcpserver.tool;

import java.util.Map;

public interface McpTool {
    String name();

    int version();

    String description();

    ToolSchema inputSchema();

    ToolSchema outputSchema();

    Object invoke(Map<String, Object> params);
}
