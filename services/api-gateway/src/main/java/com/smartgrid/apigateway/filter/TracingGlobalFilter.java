package com.smartgrid.apigateway.filter;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import com.smartgrid.apigateway.tracing.HttpHeadersCarrier;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TracingGlobalFilter implements GlobalFilter, Ordered {

    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;

    public TracingGlobalFilter(OpenTelemetry openTelemetry, Tracer tracer) {
        this.openTelemetry = openTelemetry;
        this.tracer = tracer;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        Context extractedParent = openTelemetry.getPropagators().getTextMapPropagator()
                .extract(Context.root(), request.getHeaders(), HttpHeadersCarrier.GETTER);

        Span span = tracer.spanBuilder("gateway " + request.getMethod() + " " + request.getPath().value())
                .setParent(extractedParent)
                .setSpanKind(SpanKind.SERVER)
                .startSpan();

        HttpHeaders propagatedHeaders = new HttpHeaders();
        try (Scope scope = span.makeCurrent()) {
            openTelemetry.getPropagators().getTextMapPropagator()
                    .inject(Context.current(), propagatedHeaders, HttpHeadersCarrier.SETTER);
        }

        ServerHttpRequest mutatedRequest = request.mutate()
                .headers(headers -> propagatedHeaders.forEach(headers::put))
                .build();
        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

        return chain.filter(mutatedExchange)
                .doOnSuccess(v -> {
                    var statusCode = mutatedExchange.getResponse().getStatusCode();
                    if (statusCode != null) {
                        span.setAttribute("http.status_code", statusCode.value());
                    }
                })
                .doOnError(span::recordException)
                .doFinally(signal -> span.end());
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
