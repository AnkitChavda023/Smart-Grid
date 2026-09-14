package com.smartgrid.commons.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import org.apache.kafka.common.header.Headers;

public final class KafkaTracePropagation {

    private final OpenTelemetry openTelemetry;

    public KafkaTracePropagation(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    public void inject(Context context, Headers headers) {
        openTelemetry.getPropagators().getTextMapPropagator().inject(context, headers, KafkaHeadersCarrier.SETTER);
    }

    public Context extract(Headers headers) {
        return openTelemetry.getPropagators().getTextMapPropagator()
                .extract(Context.current(), headers, KafkaHeadersCarrier.GETTER);
    }
}
