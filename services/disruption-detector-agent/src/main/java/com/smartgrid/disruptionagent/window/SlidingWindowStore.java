package com.smartgrid.disruptionagent.window;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory per-vendor sliding window of recent anomaly signals. No persistence — a restart just
 * means the window starts empty again, which is fine for a "did 2+ different signal types happen
 * recently" check; there's no requirement anywhere in the module spec for window state to survive
 * a restart, unlike the disruptions/reviews it produces, which are real persisted decisions.
 */
@Component
public class SlidingWindowStore {

    private final ConcurrentMap<String, ConcurrentLinkedDeque<Signal>> windows = new ConcurrentHashMap<>();

    public void record(String vendorId, Signal signal, Duration windowSize) {
        ConcurrentLinkedDeque<Signal> deque = windows.computeIfAbsent(vendorId, k -> new ConcurrentLinkedDeque<>());
        deque.addLast(signal);
        prune(deque, windowSize);
    }

    public List<Signal> signalsInWindow(String vendorId, Duration windowSize) {
        ConcurrentLinkedDeque<Signal> deque = windows.get(vendorId);
        if (deque == null) {
            return List.of();
        }
        prune(deque, windowSize);
        return List.copyOf(deque);
    }

    public Set<SignalType> distinctSignalTypes(String vendorId, Duration windowSize) {
        Set<SignalType> types = EnumSet.noneOf(SignalType.class);
        for (Signal signal : signalsInWindow(vendorId, windowSize)) {
            types.add(signal.type());
        }
        return types;
    }

    private void prune(ConcurrentLinkedDeque<Signal> deque, Duration windowSize) {
        Instant cutoff = Instant.now().minus(windowSize);
        deque.removeIf(signal -> signal.occurredAt().isBefore(cutoff));
    }
}
