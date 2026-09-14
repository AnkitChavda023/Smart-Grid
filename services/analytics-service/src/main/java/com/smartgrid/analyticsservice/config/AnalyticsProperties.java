package com.smartgrid.analyticsservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartgrid.analytics")
public class AnalyticsProperties {

    private long tumblingWindowMs = 3_600_000L;
    private long hoppingWindowMs = 604_800_000L;
    private long hoppingAdvanceMs = 3_600_000L;

    public long getTumblingWindowMs() {
        return tumblingWindowMs;
    }

    public void setTumblingWindowMs(long tumblingWindowMs) {
        this.tumblingWindowMs = tumblingWindowMs;
    }

    public long getHoppingWindowMs() {
        return hoppingWindowMs;
    }

    public void setHoppingWindowMs(long hoppingWindowMs) {
        this.hoppingWindowMs = hoppingWindowMs;
    }

    public long getHoppingAdvanceMs() {
        return hoppingAdvanceMs;
    }

    public void setHoppingAdvanceMs(long hoppingAdvanceMs) {
        this.hoppingAdvanceMs = hoppingAdvanceMs;
    }
}
