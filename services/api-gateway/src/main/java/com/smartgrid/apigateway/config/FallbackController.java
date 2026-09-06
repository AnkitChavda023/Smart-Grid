package com.smartgrid.apigateway.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
public class FallbackController {

    /**
     * Any HTTP method: a tripped CircuitBreaker forwards here internally while preserving the original
     * request's method (POST /orders, POST /auth/register, ...), not just GET.
     */
    @RequestMapping("/fallback/{service}")
    public Mono<ResponseEntity<Map<String, Object>>> fallback(@PathVariable String service) {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "service", service,
                "status", "CIRCUIT_OPEN",
                "message", service + " is currently unavailable; the circuit breaker has tripped",
                "timestamp", Instant.now().toString())));
    }
}
