package com.smartgrid.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayTest {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void requestWithoutJwtIsRejectedAtGatewayBeforeReachingService() {
        ResponseEntity<String> response = restTemplate.getForEntity("/orders?page=0&size=1", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void corsPreflightForProtectedRouteSucceedsWithoutAuthentication() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", "http://localhost:5173");
        headers.set("Access-Control-Request-Method", "GET");
        headers.set("Access-Control-Request-Headers", "authorization");

        ResponseEntity<Void> response = restTemplate.exchange(
                "/orders", HttpMethod.OPTIONS, new HttpEntity<>(headers), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("Access-Control-Allow-Origin")).isEqualTo("http://localhost:5173");
    }

    @Test
    void validJwtRoutesThroughGatewayToOrderService() {
        String token = obtainToken("planner-" + UUID.randomUUID());

        ResponseEntity<String> response = restTemplate.exchange(
                "/orders?page=0&size=1", HttpMethod.GET, authenticated(token), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void rateLimiterBlocksClientAfterBurstCapacityExceeded() {
        String token = obtainToken("burst-" + UUID.randomUUID());
        HttpEntity<Void> request = authenticated(token);

        boolean sawTooManyRequests = false;
        for (int i = 0; i < 25 && !sawTooManyRequests; i++) {
            ResponseEntity<String> response = restTemplate.exchange(
                    "/orders?page=0&size=1", HttpMethod.GET, request, String.class);
            if (response.getStatusCode().value() == 429) {
                sawTooManyRequests = true;
            }
        }

        assertThat(sawTooManyRequests).isTrue();
    }

    @Test
    void circuitBreakerOpensAfterRepeatedDownstreamFailuresAndServesFallback() {
        String token = obtainToken("cb-" + UUID.randomUUID());
        HttpEntity<Void> request = authenticated(token);

        String lastBody = null;
        for (int i = 0; i < 15; i++) {
            ResponseEntity<String> response = restTemplate.exchange(
                    "/diagnostics/unreachable/ping", HttpMethod.GET, request, String.class);
            lastBody = response.getBody();
        }

        assertThat(lastBody).contains("CIRCUIT_OPEN");
    }

    @Test
    void traceIdFromGatewayAppearsAcrossThreeDownstreamServiceSpans() throws Exception {
        String token = obtainToken("trace-" + UUID.randomUUID());
        String traceId = randomHex(32);
        String traceparent = "00-" + traceId + "-" + randomHex(16) + "-01";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("traceparent", traceparent);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        restTemplate.exchange("/orders?page=0&size=1", HttpMethod.GET, request, String.class);
        restTemplate.exchange("/vendors/top?sku=sku-1&k=1", HttpMethod.GET, request, String.class);
        restTemplate.exchange("/contracts/search?q=x", HttpMethod.GET, request, String.class);

        Set<String> expected = Set.of("order-service", "vendor-service", "contract-service");
        Set<String> serviceNames = pollZipkinForServiceNames(traceId, expected, Duration.ofSeconds(25));

        assertThat(serviceNames).containsAll(expected);
    }

    @SuppressWarnings("unchecked")
    private Set<String> pollZipkinForServiceNames(String traceId, Set<String> expectedServiceNames, Duration timeout)
            throws Exception {
        RestTemplate zipkinClient = new RestTemplate();
        Instant deadline = Instant.now().plus(timeout);
        Set<String> serviceNames = Set.of();

        while (Instant.now().isBefore(deadline)) {
            try {
                List<Map<String, Object>> trace = zipkinClient.getForObject(
                        "http://localhost:9411/api/v2/trace/" + traceId, List.class);
                if (trace != null && !trace.isEmpty()) {
                    serviceNames = trace.stream()
                            .map(span -> (Map<String, Object>) span.get("localEndpoint"))
                            .filter(endpoint -> endpoint != null)
                            .map(endpoint -> (String) endpoint.get("serviceName"))
                            .collect(java.util.stream.Collectors.toSet());
                    if (serviceNames.containsAll(expectedServiceNames)) {
                        return serviceNames;
                    }
                }
            } catch (Exception e) {
                // Zipkin hasn't received/flushed the batch yet; keep polling until the timeout.
            }
            Thread.sleep(1000);
        }
        return serviceNames;
    }

    private String randomHex(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(Integer.toHexString(RANDOM.nextInt(16)));
        }
        return sb.toString();
    }

    private HttpEntity<Void> authenticated(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return new HttpEntity<>(headers);
    }

    private String obtainToken(String username) {
        Map<String, Object> registerBody = Map.of(
                "username", username, "password", "password123", "role", "PLANNER");
        ResponseEntity<Void> registerResponse = restTemplate.postForEntity("/auth/register", registerBody, Void.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map<String, Object> loginBody = Map.of("username", username, "password", "password123");
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity("/auth/login", loginBody, Map.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        return (String) loginResponse.getBody().get("accessToken");
    }
}
