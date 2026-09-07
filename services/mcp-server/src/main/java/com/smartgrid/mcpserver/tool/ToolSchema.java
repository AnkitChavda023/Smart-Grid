package com.smartgrid.mcpserver.tool;

import java.util.List;
import java.util.Map;

/** A minimal JSON Schema subset — enough for LangChain4j (or any function-calling client) to parse parameter shape. */
public record ToolSchema(String type, Map<String, PropertySchema> properties, List<String> required) {

    public record PropertySchema(String type, String description) {
    }

    public static ToolSchema object(Map<String, PropertySchema> properties, List<String> required) {
        return new ToolSchema("object", properties, required);
    }
}
