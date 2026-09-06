package com.smartgrid.apigateway.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayTracingConfig {

    private static final AttributeKey<String> SERVICE_NAME_KEY = AttributeKey.stringKey("service.name");

    @Bean
    public OpenTelemetry openTelemetry(
            @Value("${smartgrid.tracing.service-name}") String serviceName,
            @Value("${smartgrid.tracing.zipkin-endpoint}") String zipkinEndpoint) {
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(SERVICE_NAME_KEY, serviceName)));

        ZipkinSpanExporter zipkinExporter = ZipkinSpanExporter.builder()
                .setEndpoint(zipkinEndpoint)
                .build();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(zipkinExporter).build())
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();
    }

    @Bean
    public Tracer gatewayTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("api-gateway");
    }
}
