package com.smartgrid.mcpserver.config;

import com.smartgrid.mcpserver.security.InternalTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final InternalTokenInterceptor internalTokenInterceptor;

    public WebMvcConfig(InternalTokenInterceptor internalTokenInterceptor) {
        this.internalTokenInterceptor = internalTokenInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(internalTokenInterceptor)
                .addPathPatterns("/v1/tools/**", "/v2/tools/**", "/tools/**");
    }
}
