package com.smartgrid.ragservice.ingest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkerTest {

    private final TextChunker chunker = new TextChunker();

    @Test
    void shortTextReturnsSingleChunk() {
        List<String> chunks = chunker.chunk("Short capability description.", 500, 50);
        assertThat(chunks).containsExactly("Short capability description.");
    }

    @Test
    void blankTextReturnsNoChunks() {
        assertThat(chunker.chunk("   ", 500, 50)).isEmpty();
    }

    @Test
    void longTextSplitsOnSentenceBoundaries() {
        String sentence = "This vendor specializes in high voltage transformer manufacturing. ";
        String text = sentence.repeat(20);

        List<String> chunks = chunker.chunk(text, 200, 20);

        assertThat(chunks.size()).isGreaterThan(1);
        for (String chunk : chunks) {
            assertThat(chunk).matches(".*[.!?]$");
        }
    }

    @Test
    void adjacentChunksOverlapByPosition() {
        String sentence = "Sentence number %d covers a distinct topic in the catalog. ";
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 15; i++) {
            text.append(String.format(sentence, i));
        }
        String fullText = text.toString();

        List<String> chunks = chunker.chunk(fullText, 150, 30);

        assertThat(chunks.size()).isGreaterThan(1);
        int previousStart = fullText.indexOf(chunks.get(0));
        int previousEnd = previousStart + chunks.get(0).length();
        for (int i = 1; i < chunks.size(); i++) {
            int start = fullText.indexOf(chunks.get(i));
            assertThat(start).isLessThan(previousEnd);
            previousEnd = start + chunks.get(i).length();
        }
    }
}
