package com.smartgrid.mcpserver.tool;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Component
public class ToolRegistry {

    private final Map<String, TreeMap<Integer, McpTool>> toolsByNameThenVersion = new java.util.HashMap<>();

    public ToolRegistry(List<McpTool> tools) {
        for (McpTool tool : tools) {
            toolsByNameThenVersion
                    .computeIfAbsent(tool.name(), n -> new TreeMap<>())
                    .put(tool.version(), tool);
        }
    }

    public Optional<McpTool> resolve(String name, int version) {
        return Optional.ofNullable(toolsByNameThenVersion.get(name)).map(versions -> versions.get(version));
    }

    public Optional<McpTool> resolveLatest(String name) {
        return Optional.ofNullable(toolsByNameThenVersion.get(name)).map(TreeMap::lastEntry).map(Map.Entry::getValue);
    }

    public List<ToolDefinition> listAtVersion(int version) {
        return toolsByNameThenVersion.values().stream()
                .map(versions -> versions.get(version))
                .filter(java.util.Objects::nonNull)
                .map(ToolDefinition::of)
                .toList();
    }
}
