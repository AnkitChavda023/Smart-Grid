package com.smartgrid.commons.web;

import org.slf4j.MDC;

import java.util.UUID;

public final class CorrelationContext {

    public static final String HEADER_NAME = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";

    private CorrelationContext() {
    }

    public static String generate() {
        return UUID.randomUUID().toString();
    }

    public static void set(String correlationId) {
        MDC.put(MDC_KEY, correlationId);
    }

    public static String get() {
        String correlationId = MDC.get(MDC_KEY);
        return correlationId != null ? correlationId : generate();
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}
