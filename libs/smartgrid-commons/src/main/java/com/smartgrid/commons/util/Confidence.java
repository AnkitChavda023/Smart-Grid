package com.smartgrid.commons.util;

public final class Confidence {

    private Confidence() {
    }

    /**
     * LLM-reported confidence is meant to be a 0.0-1.0 probability, but a model can return a value
     * outside that range (e.g. answering "how confident, 1-10?" literally instead of as a fraction).
     * Since this value gates whether an agent decision auto-publishes or escalates to a human, clamp
     * it defensively rather than trusting the model's raw output for a safety-relevant threshold check.
     */
    public static double clamp(double rawConfidence) {
        return Math.max(0.0, Math.min(1.0, rawConfidence));
    }
}
