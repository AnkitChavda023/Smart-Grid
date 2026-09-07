package com.smartgrid.mcpserver;

import com.smartgrid.mcpserver.security.InternalTokenInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpServerTest {

    private static final String TOKEN = "local-dev-internal-token";

    // Apex Grid Transformers â€” real vendor seeded ahead of this test run via a direct POST /vendors call.
    private static final String SEEDED_VENDOR_ID = "f4edbdae-6c1a-4497-807a-fb2a6e347ba2";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void callWithoutTokenReturns403() {
        ResponseEntity<String> response = restTemplate.getForEntity("/v1/tools", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void callWithWrongTokenReturns403() {
        HttpEntity<Void> request = authenticated("wrong-token");
        ResponseEntity<String> response = restTemplate.exchange("/v1/tools", HttpMethod.GET, request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void v1AndV2SearchVendorsBothAccessibleSimultaneously() {
        HttpEntity<Void> request = authenticated(TOKEN);

        ResponseEntity<String> v1Schema = restTemplate.exchange("/v1/tools/searchVendors", HttpMethod.GET, request, String.class);
        ResponseEntity<String> v2Schema = restTemplate.exchange("/v2/tools/searchVendors", HttpMethod.GET, request, String.class);

        assertThat(v1Schema.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(v2Schema.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(v1Schema.getBody()).doesNotContain("minReliability");
        assertThat(v2Schema.getBody()).contains("minReliability");
    }

    @Test
    void allEighteenToolsListedAtV1() {
        // 8 from M15 + 10 from M17/M18 (updateVendorHealthReport, getVendorRiskProfile, triggerPreemptiveRestock,
        // notifyProcurementManager, alertPlannerForReorder, notifyPlannerForReview, updateSafetyStockLevel,
        // getActiveContract, getMarketBenchmarks, saveDraftContract) — deliberately no submitContract tool.
        HttpEntity<Void> request = authenticated(TOKEN);
        ResponseEntity<List> response = restTemplate.exchange("/v1/tools", HttpMethod.GET, request, List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(18);
    }

    @Test
    void submitContractIsNeverExposedAsATool() {
        // M18's hard rule: submitContract must never be MCP-callable, only reachable via a real
        // PLANNER/ADMIN JWT calling contract-service's POST /contracts/drafts/{id}/submit directly.
        HttpEntity<Void> request = authenticated(TOKEN);
        ResponseEntity<String> response = restTemplate.exchange("/v1/tools/submitContract", HttpMethod.GET, request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void searchVendorsToolReturnsRankedRealVendors() {
        HttpEntity<Map<String, Object>> request = authenticatedBody(TOKEN, Map.of("sku", "sku-transformer-500kv"));
        ResponseEntity<List> response = restTemplate.exchange("/v1/tools/searchVendors", HttpMethod.POST, request, List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    void estimateLeadTimeToolAveragesRealVendorSkus() {
        HttpEntity<Map<String, Object>> request = authenticatedBody(TOKEN, Map.of("vendorId", SEEDED_VENDOR_ID));
        ResponseEntity<Map> response = restTemplate.exchange("/v1/tools/estimateLeadTime", HttpMethod.POST, request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("skuCount")).isEqualTo(1);
        assertThat(((Number) response.getBody().get("averageLeadTimeDays")).doubleValue()).isEqualTo(21.0);
    }

    @Test
    void getBreachHistoryToolCallsRealContractService() {
        HttpEntity<Map<String, Object>> request = authenticatedBody(TOKEN, Map.of("vendorId", SEEDED_VENDOR_ID));
        ResponseEntity<List> response = restTemplate.exchange("/v1/tools/getBreachHistory", HttpMethod.POST, request, List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void toolSubmitDispatchesToLatestVersionByDefault() {
        HttpEntity<Map<String, Object>> request = authenticatedBody(TOKEN,
                Map.of("toolName", "searchVendors", "params", Map.of("sku", "sku-transformer-500kv", "minReliability", 0.0)));
        ResponseEntity<String> response = restTemplate.exchange("/tools/submit", HttpMethod.POST, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void missingRequiredParamReturns400() {
        HttpEntity<Map<String, Object>> request = authenticatedBody(TOKEN, Map.of());
        ResponseEntity<String> response = restTemplate.exchange("/v1/tools/searchVendors", HttpMethod.POST, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private HttpEntity<Void> authenticated(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(InternalTokenInterceptor.TOKEN_HEADER, token);
        return new HttpEntity<>(headers);
    }

    private <T> HttpEntity<T> authenticatedBody(String token, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(InternalTokenInterceptor.TOKEN_HEADER, token);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
