package com.smartgrid.contractagent;

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
 * word-level LCS clause-diff is pure computation, fully real and independently verifiable; only the
 * contract-drafting LLM call is blocked on OpenAI billing. See docs/modules/M18-contract-agent-observability.md.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContractNegotiationAgentTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void negotiationsForUnknownVendorReturnsEmptyList() {
        ResponseEntity<List> response = restTemplate.getForEntity("/negotiations/{id}", List.class, UUID.randomUUID());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void simulateWithMissingVendorIdReturns400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("{\"vendorId\":\"\"}", headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/negotiations/simulate", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
