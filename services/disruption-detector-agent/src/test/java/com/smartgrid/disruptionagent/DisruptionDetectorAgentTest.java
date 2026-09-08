package com.smartgrid.disruptionagent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the REST surface and persistence that don't require a real LLM completion call. The
 * confidence-gated analysis pipeline itself (sliding window -> vendor/RAG context -> LLM -> publish)
 * is fully wired and was exercised live via /disruptions/simulate against the real running stack —
 * it correctly reaches rag-service's embedding call and fails there on the account's exhausted
 * OpenAI credits, the same wall M14 is blocked on. See docs/modules/M16-core-agents.md.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DisruptionDetectorAgentTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void activeDisruptionsIsAccessibleAndReturnsAList() {
        ResponseEntity<List> response = restTemplate.getForEntity("/disruptions/active", List.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void reasoningForUnknownDisruptionReturns404() {
        ResponseEntity<String> response = restTemplate.getForEntity("/disruptions/{id}/reasoning", String.class, UUID.randomUUID());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void simulateWithMissingVendorIdReturns400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(
                "{\"vendorId\":\"\",\"signals\":[{\"type\":\"SLA_BREACH\",\"severity\":1.0,\"description\":\"x\"}]}", headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/disruptions/simulate", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
