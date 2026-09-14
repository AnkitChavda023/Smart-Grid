package com.smartgrid.disruptionagent.window;

import java.time.Instant;

public record Signal(Instant occurredAt, SignalType type, double severity, String description) {
}
