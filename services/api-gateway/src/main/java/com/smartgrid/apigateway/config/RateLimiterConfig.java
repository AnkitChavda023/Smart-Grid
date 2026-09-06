package com.smartgrid.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class RateLimiterConfig {

    /**
     * Per-API-key (JWT subject) for authenticated requests, falling back to per-client-IP for the
     * unauthenticated public routes (login/register/refresh) where no JWT subject exists yet.
     */
    @Bean
    public KeyResolver apiKeyOrIpKeyResolver() {
        return exchange -> ReactiveSecurityContextHolder.getContext()
                .flatMap(ctx -> Mono.justOrEmpty(ctx.getAuthentication()))
                .map(auth -> auth instanceof JwtAuthenticationToken jwtAuth
                        ? "user:" + jwtAuth.getToken().getSubject()
                        : "ip:" + resolveClientIp(exchange))
                .switchIfEmpty(Mono.fromSupplier(() -> "ip:" + resolveClientIp(exchange)));
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress == null || remoteAddress.getAddress() == null
                ? "unknown"
                : remoteAddress.getAddress().getHostAddress();
    }
}
