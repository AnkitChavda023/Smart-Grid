package com.smartgrid.mcpserver.web;

import com.smartgrid.commons.exception.ResourceNotFoundException;
import com.smartgrid.commons.exception.ValidationException;
import com.smartgrid.mcpserver.tool.McpTool;
import com.smartgrid.mcpserver.tool.ToolRegistry;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ToolSubmitController {

    private final ToolRegistry registry;

    public ToolSubmitController(ToolRegistry registry) {
        this.registry = registry;
    }

    /** Generic single entry point: dispatches by tool name to its latest registered version, unless a version is given explicitly. */
    @PostMapping("/tools/submit")
    public Object submit(@RequestBody ToolSubmitRequest request) {
        if (request.toolName() == null || request.toolName().isBlank()) {
            throw new ValidationException("toolName is required");
        }
        McpTool tool = (request.version() == null ? registry.resolveLatest(request.toolName()) : registry.resolve(request.toolName(), request.version()))
                .orElseThrow(() -> new ResourceNotFoundException("Tool", request.toolName()
                        + (request.version() == null ? "" : "@v" + request.version())));
        return tool.invoke(request.params() == null ? Map.of() : request.params());
    }

    public record ToolSubmitRequest(String toolName, Integer version, Map<String, Object> params) {
    }
}
