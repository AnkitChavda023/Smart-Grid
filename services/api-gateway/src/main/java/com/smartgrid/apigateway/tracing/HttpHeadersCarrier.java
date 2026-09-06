package com.smartgrid.apigateway.tracing;

import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.springframework.http.HttpHeaders;

public final class HttpHeadersCarrier {

    public static final TextMapSetter<HttpHeaders> SETTER = HttpHeaders::set;

    public static final TextMapGetter<HttpHeaders> GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(HttpHeaders headers) {
            return headers.keySet();
        }

        @Override
        public String get(HttpHeaders headers, String key) {
            return headers == null ? null : headers.getFirst(key);
        }
    };

    private HttpHeadersCarrier() {
    }
}
