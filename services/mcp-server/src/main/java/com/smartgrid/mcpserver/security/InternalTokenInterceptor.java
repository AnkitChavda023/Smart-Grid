package com.smartgrid.mcpserver.security;

import com.smartgrid.mcpserver.config.McpProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class InternalTokenInterceptor implements HandlerInterceptor {

    public static final String TOKEN_HEADER = "X-Internal-Token";

    private final McpProperties properties;

    public InternalTokenInterceptor(McpProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws java.io.IOException {
        String token = request.getHeader(TOKEN_HEADER);
        if (token == null || !token.equals(properties.internalToken())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing or invalid internal service token");
            return false;
        }
        return true;
    }
}
