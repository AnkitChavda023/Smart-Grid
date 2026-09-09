package com.smartgrid.contractagent.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ClauseDiffAnalyzerTest {

    private final ClauseDiffAnalyzer analyzer = new ClauseDiffAnalyzer();

    @Test
    void identicalTextIsFullySimilar() {
        String text = "vendor shall deliver goods within thirty days";
        assertThat(analyzer.similarity(text, text)).isCloseTo(1.0, within(0.0001));
    }

    @Test
    void completelyDifferentTextIsNotSimilar() {
        assertThat(analyzer.similarity("alpha beta gamma", "delta epsilon zeta")).isZero();
    }

    @Test
    void oneWordChangeIsMostlySimilar() {
        String current = "vendor shall deliver goods within thirty days";
        String proposed = "vendor shall deliver goods within fifteen days";
        double similarity = analyzer.similarity(current, proposed);
        assertThat(similarity).isGreaterThan(0.8).isLessThan(1.0);
    }

    @Test
    void emptyTextIsNotSimilarToAnything() {
        assertThat(analyzer.similarity("", "some real terms")).isZero();
        assertThat(analyzer.similarity(null, "some real terms")).isZero();
    }
}
