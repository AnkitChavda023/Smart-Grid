package com.smartgrid.ragservice.ingest;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sentence-boundary-aware fixed-size chunking with overlap. Most source fields in this project
 * (vendor capabilities, contract terms, breach reasons) are short — under maxChars — and come
 * back as a single chunk; the sliding window only matters once documents grow past that size.
 */
@Component
public class TextChunker {

    public List<String> chunk(String text, int maxChars, int overlapChars) {
        String normalized = text.strip();
        if (normalized.isEmpty()) {
            return List.of();
        }
        if (normalized.length() <= maxChars) {
            return List.of(normalized);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + maxChars, normalized.length());
            int breakAt = lastSentenceBoundary(normalized, start, end);
            chunks.add(normalized.substring(start, breakAt).strip());
            if (breakAt >= normalized.length()) {
                break;
            }
            start = Math.max(breakAt - overlapChars, start + 1);
        }
        return chunks;
    }

    private int lastSentenceBoundary(String text, int start, int end) {
        for (int i = end - 1; i > start; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?') {
                return i + 1;
            }
        }
        return end;
    }
}
