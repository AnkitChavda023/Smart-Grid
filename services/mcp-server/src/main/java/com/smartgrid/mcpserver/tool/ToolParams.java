package com.smartgrid.mcpserver.tool;

import com.smartgrid.commons.exception.ValidationException;

import java.util.Map;

final class ToolParams {

    private ToolParams() {
    }

    static String requireString(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new ValidationException("Missing required param: " + key);
        }
        return value.toString();
    }

    static String optionalString(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value == null ? null : value.toString();
    }

    static int requireInt(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new ValidationException("Missing required param: " + key);
        }
        return ((Number) value).intValue();
    }

    static int optionalInt(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        return value == null ? defaultValue : ((Number) value).intValue();
    }

    static double optionalDouble(Map<String, Object> params, String key, double defaultValue) {
        Object value = params.get(key);
        return value == null ? defaultValue : ((Number) value).doubleValue();
    }
}
