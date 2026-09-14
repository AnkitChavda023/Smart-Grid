package com.smartgrid.commons.tracing;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartgrid.tracing")
public class SmartGridTracingProperties {

    private String serviceName;
    private String zipkinEndpoint = "http://localhost:9411/api/v2/spans";

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getZipkinEndpoint() {
        return zipkinEndpoint;
    }

    public void setZipkinEndpoint(String zipkinEndpoint) {
        this.zipkinEndpoint = zipkinEndpoint;
    }
}
