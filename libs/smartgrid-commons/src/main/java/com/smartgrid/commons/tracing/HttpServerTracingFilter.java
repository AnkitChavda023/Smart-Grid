package com.smartgrid.commons.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

public class HttpServerTracingFilter extends OncePerRequestFilter {

    private static final TextMapGetter<HttpServletRequest> GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(HttpServletRequest request) {
            return Collections.list(request.getHeaderNames());
        }

        @Override
        public String get(HttpServletRequest request, String key) {
            return request == null ? null : request.getHeader(key);
        }
    };

    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;

    public HttpServerTracingFilter(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        this.tracer = openTelemetry.getTracer("http-server");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Context extractedParent = openTelemetry.getPropagators().getTextMapPropagator()
                .extract(Context.root(), request, GETTER);

        Span span = tracer.spanBuilder(request.getMethod() + " " + request.getRequestURI())
                .setParent(extractedParent)
                .setSpanKind(SpanKind.SERVER)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            filterChain.doFilter(request, response);
            span.setAttribute("http.status_code", response.getStatus());
        } finally {
            span.end();
        }
    }
}
