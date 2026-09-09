package com.smartgrid.forecasteragent;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the REST surface and persistence that don't require a real LLM completion call — the
 * exponential-smoothing baseline and P10/P50/P90 statistics are pure computation, fully real and
 * independently verifiable; only the seasonal-adjustment LLM call is blocked on OpenAI billing.
 * See docs/modules/M17-extended-agents.md.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemandForecasterAgentTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void forecastsForUnknownSkuReturnsEmptyList() {
        ResponseEntity<List> response = restTemplate.getForEntity("/demand-forecasts/{sku}", List.class, "sku-does-not-exist");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void simulateWithMissingSkuIdReturns400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("{\"skuId\":\"\"}", headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/demand-forecasts/simulate", request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
