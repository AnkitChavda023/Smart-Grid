package com.smartgrid.apigateway.config;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    /**
     * Every downstream route gets its own named CircuitBreaker + TimeLimiter instance (matching the
     * route id, e.g. "order-service"), so one noisy service tripping OPEN never affects another route.
     */
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> circuitBreakerCustomizer() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
        TimeLimiterConfig defaultTimeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(8))
                .build();
        TimeLimiterConfig agentTimeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMinutes(3))
                .build();

        return factory -> factory.configureDefault(id -> {
            boolean isAgent = id != null && id.contains("agent");
            return new Resilience4JConfigBuilder(id)
                    .circuitBreakerConfig(circuitBreakerConfig)
                    .timeLimiterConfig(isAgent ? agentTimeLimiterConfig : defaultTimeLimiterConfig)
                    .build();
        });
    }

    /**
     * Spring Cloud Circuit Breaker's reactive Resilience4j integration only wires up CircuitBreaker +
     * TimeLimiter, not Bulkhead, so bulkhead isolation is applied separately via a custom "Bulkhead"
     * GatewayFilter (see BulkheadGatewayFilterFactory) backed by this registry.
     *
     * Semaphore-based Bulkhead, not ThreadPoolBulkhead: the gateway runs on Netty's small, fixed
     * event-loop pool, so handing each downstream service its own OS thread pool would fight the
     * reactive, non-blocking model this gateway is built on. A semaphore bulkhead caps concurrent
     * in-flight calls per route with no extra threads, giving the same noisy-neighbor isolation.
     */
    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        BulkheadConfig defaultConfig = BulkheadConfig.custom()
                .maxConcurrentCalls(20)
                .maxWaitDuration(Duration.ZERO)
                .build();
        return BulkheadRegistry.of(defaultConfig);
    }
}
