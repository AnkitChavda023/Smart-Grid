package com.smartgrid.commons.tracing;

import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;

public final class KafkaHeadersCarrier {

    public static final TextMapSetter<Headers> SETTER =
            (headers, key, value) -> headers.remove(key).add(key, value.getBytes(StandardCharsets.UTF_8));

    public static final TextMapGetter<Headers> GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Headers headers) {
            return () -> {
                var iterator = headers.iterator();
                return new java.util.Iterator<String>() {
                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public String next() {
                        return iterator.next().key();
                    }
                };
            };
        }

        @Override
        public String get(Headers headers, String key) {
            if (headers == null) {
                return null;
            }
            Header header = headers.lastHeader(key);
            return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
        }
    };

    private KafkaHeadersCarrier() {
    }
}
