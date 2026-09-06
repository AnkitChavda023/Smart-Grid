package com.smartgrid.apigateway.filter;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

/**
 * Registered under the name "Bulkhead" (Spring Cloud Gateway strips the "GatewayFilterFactory" suffix),
 * usable in application.yml as `- name: Bulkhead / args: name: <route-id>`. Spring Cloud Circuit
 * Breaker's reactive Resilience4j support has no built-in bulkhead wiring, so this composes Resilience4j's
 * own reactor BulkheadOperator directly onto the route's filter chain.
 */
@Component
public class BulkheadGatewayFilterFactory extends AbstractGatewayFilterFactory<BulkheadGatewayFilterFactory.Config> {

    private final BulkheadRegistry bulkheadRegistry;

    public BulkheadGatewayFilterFactory(BulkheadRegistry bulkheadRegistry) {
        super(Config.class);
        this.bulkheadRegistry = bulkheadRegistry;
    }

    @Override
    public GatewayFilter apply(Config config) {
        Bulkhead bulkhead = bulkheadRegistry.bulkhead(config.getName());
        return (exchange, chain) -> chain.filter(exchange)
                .transformDeferred(BulkheadOperator.of(bulkhead));
    }

    public static class Config {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
