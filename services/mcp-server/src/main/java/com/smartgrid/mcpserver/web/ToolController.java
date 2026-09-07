package com.smartgrid.mcpserver.web;

import com.smartgrid.commons.exception.ResourceNotFoundException;
import com.smartgrid.mcpserver.tool.McpTool;
import com.smartgrid.mcpserver.tool.ToolDefinition;
import com.smartgrid.mcpserver.tool.ToolRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class ToolController {

    private final ToolRegistry registry;

    public ToolController(ToolRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/v{version}/tools")
    public List<ToolDefinition> list(@PathVariable int version) {
        return registry.listAtVersion(version);
    }

    @GetMapping("/v{version}/tools/{name}")
    public ToolDefinition schema(@PathVariable int version, @PathVariable String name) {
        return registry.resolve(name, version)
                .map(ToolDefinition::of)
                .orElseThrow(() -> new ResourceNotFoundException("Tool", name + "@v" + version));
    }

    @PostMapping("/v{version}/tools/{name}")
    public Object invoke(@PathVariable int version, @PathVariable String name,
                          @RequestBody(required = false) Map<String, Object> params) {
        McpTool tool = registry.resolve(name, version)
                .orElseThrow(() -> new ResourceNotFoundException("Tool", name + "@v" + version));
        return tool.invoke(params == null ? Map.of() : params);
    }
}
