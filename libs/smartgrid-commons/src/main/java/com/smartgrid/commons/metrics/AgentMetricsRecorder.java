package com.smartgrid.commons.metrics;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * The 4 metrics every agent (M16-M18) exposes, in one place so each agent doesn't reinvent the
 * same counter/histogram wiring. Each metric is tagged by {@code agent} so a single Grafana panel
 * can show all 6 agents at once, per the module spec.
 */
public class AgentMetricsRecorder {

    private final MeterRegistry registry;
    private final String agentName;

    public AgentMetricsRecorder(MeterRegistry registry, String agentName) {
        this.registry = registry;
        this.agentName = agentName;
    }

    public void recordToolCall(String tool) {
        registry.counter("agent_tool_calls_total", "agent", agentName, "tool", tool).increment();
    }

    public void recordConfidence(double confidence) {
        DistributionSummary.builder("agent_confidence_score")
                .tag("agent", agentName)
                .register(registry)
                .record(confidence);
    }

    public void recordEscalation() {
        registry.counter("agent_escalation_total", "agent", agentName).increment();
    }

    public Timer.Sample startLatency() {
        return Timer.start(registry);
    }

    public void stopLatency(Timer.Sample sample) {
        sample.stop(Timer.builder("agent_latency_seconds").tag("agent", agentName).register(registry));
    }
}
