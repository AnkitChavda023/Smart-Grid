package com.smartgrid.commons.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Only activates for services that actually have a MeterRegistry — i.e. depend on
 * spring-boot-starter-actuator. {@code @AutoConfigureAfter(name=...)} (string class names, not
 * {@code Class<?>} references, since actuator is optional here) is required, not optional:
 * {@code @ConditionalOnBean} alone doesn't reliably see beans from auto-configuration classes
 * contributed by a *different* jar's AutoConfiguration.imports without explicit ordering — found by
 * a real context-load failure when this was first wired into the M16 agents.
 */
@AutoConfiguration(
        afterName = {
                "org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration",
                "org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration"
        }
)
@ConditionalOnBean(MeterRegistry.class)
public class MetricsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AgentMetricsRecorder agentMetricsRecorder(MeterRegistry registry, @Value("${spring.application.name}") String agentName) {
        return new AgentMetricsRecorder(registry, agentName);
    }
}
