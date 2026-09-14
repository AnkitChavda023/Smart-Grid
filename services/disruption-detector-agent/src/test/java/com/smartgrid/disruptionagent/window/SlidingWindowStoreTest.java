package com.smartgrid.disruptionagent.window;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SlidingWindowStoreTest {

    private final SlidingWindowStore store = new SlidingWindowStore();

    @Test
    void singleSignalTypeDoesNotReachTwoDistinctTypes() {
        store.record("vendor-1", new Signal(Instant.now(), SignalType.SLA_BREACH, 1.0, "breach"), Duration.ofMinutes(30));
        store.record("vendor-1", new Signal(Instant.now(), SignalType.SLA_BREACH, 1.0, "breach again"), Duration.ofMinutes(30));

        assertThat(store.distinctSignalTypes("vendor-1", Duration.ofMinutes(30))).containsExactly(SignalType.SLA_BREACH);
    }

    @Test
    void twoDifferentSignalTypesBothCountTowardTheWindow() {
        store.record("vendor-2", new Signal(Instant.now(), SignalType.SLA_BREACH, 1.0, "breach"), Duration.ofMinutes(30));
        store.record("vendor-2", new Signal(Instant.now(), SignalType.SCORE_DROP, 0.2, "score drop"), Duration.ofMinutes(30));

        assertThat(store.distinctSignalTypes("vendor-2", Duration.ofMinutes(30)))
                .containsExactlyInAnyOrder(SignalType.SLA_BREACH, SignalType.SCORE_DROP);
    }

    @Test
    void signalsOutsideTheWindowArePruned() {
        Signal old = new Signal(Instant.now().minus(Duration.ofHours(2)), SignalType.SLA_BREACH, 1.0, "old breach");
        store.record("vendor-3", old, Duration.ofMinutes(30));

        assertThat(store.signalsInWindow("vendor-3", Duration.ofMinutes(30))).isEmpty();
        assertThat(store.distinctSignalTypes("vendor-3", Duration.ofMinutes(30))).isEmpty();
    }

    @Test
    void unknownVendorHasEmptyWindow() {
        assertThat(store.signalsInWindow("never-seen", Duration.ofMinutes(30))).isEmpty();
        assertThat(store.distinctSignalTypes("never-seen", Duration.ofMinutes(30))).isEmpty();
    }

    @Test
    void differentVendorsHaveIndependentWindows() {
        store.record("vendor-a", new Signal(Instant.now(), SignalType.VENDOR_SUSPENSION, 1.0, "suspended"), Duration.ofMinutes(30));

        assertThat(store.distinctSignalTypes("vendor-a", Duration.ofMinutes(30))).containsExactly(SignalType.VENDOR_SUSPENSION);
        assertThat(store.distinctSignalTypes("vendor-b", Duration.ofMinutes(30))).isEmpty();
    }
}
