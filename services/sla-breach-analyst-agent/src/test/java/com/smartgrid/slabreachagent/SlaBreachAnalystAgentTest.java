package com.smartgrid.slabreachagent;

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
 * Covers the REST surface and persistence that don't require a real LLM completion call — the
 * Poisson breach-probability math and preemptive-restock gating are pure computation, fully real,
 * and independently verifiable; only the LLM confidence/summary call is blocked on OpenAI billing.
 * See docs/modules/M17-extended-agents.md.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SlaBreachAnalystAgentTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void assessmentsForUnknownVendorReturnsEmptyList() {
        ResponseEntity<List> response = restTemplate.getForEntity("/breach-assessments/{id}", List.class, UUID.randomUUID());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void simulateWithMissingVendorIdReturns400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("{\"vendorId\":\"\"}", headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/breach-assessments/simulate", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
