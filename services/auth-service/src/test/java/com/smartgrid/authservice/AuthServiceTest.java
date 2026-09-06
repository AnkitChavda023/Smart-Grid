package com.smartgrid.authservice;

import com.smartgrid.authservice.domain.Role;
import com.smartgrid.authservice.dto.LoginRequest;
import com.smartgrid.authservice.dto.RefreshRequest;
import com.smartgrid.authservice.dto.RegisterRequest;
import com.smartgrid.authservice.dto.TokenPairResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthServiceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void fullAuthLifecycle() {
        String username = "supplier-" + UUID.randomUUID();
        register(username, Role.SUPPLIER);

        ResponseEntity<String> unauthorized = restTemplate.getForEntity("/auth/me", String.class);
        assertThat(unauthorized.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        TokenPairResponse tokens = login(username);
        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();

        ResponseEntity<Map> me = restTemplate.exchange("/auth/me", HttpMethod.GET, authorized(tokens.accessToken()), Map.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).containsEntry("username", username);

        ResponseEntity<String> forbidden = restTemplate.exchange("/auth/admin/ping", HttpMethod.GET, authorized(tokens.accessToken()), String.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        RefreshRequest refreshRequest = new RefreshRequest(tokens.refreshToken());
        ResponseEntity<TokenPairResponse> refreshResponse = restTemplate.postForEntity("/auth/refresh", refreshRequest, TokenPairResponse.class);
        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TokenPairResponse rotated = refreshResponse.getBody();
        assertThat(rotated.refreshToken()).isNotEqualTo(tokens.refreshToken());

        ResponseEntity<String> reuseOldRefresh = restTemplate.postForEntity("/auth/refresh", refreshRequest, String.class);
        assertThat(reuseOldRefresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        RefreshRequest logoutRequest = new RefreshRequest(rotated.refreshToken());
        ResponseEntity<Void> logoutResponse = restTemplate.postForEntity("/auth/logout", logoutRequest, Void.class);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> refreshAfterLogout = restTemplate.postForEntity("/auth/refresh", logoutRequest, String.class);
        assertThat(refreshAfterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void adminRoleCanAccessAdminEndpoint() {
        String username = "admin-" + UUID.randomUUID();
        register(username, Role.ADMIN);
        TokenPairResponse tokens = login(username);

        ResponseEntity<Map> response = restTemplate.exchange("/auth/admin/ping", HttpMethod.GET, authorized(tokens.accessToken()), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void repeatedFailedLoginsTriggerLockout() {
        String username = "locktest-" + UUID.randomUUID();
        register(username, Role.PLANNER);

        for (int i = 0; i < 3; i++) {
            ResponseEntity<String> failed = restTemplate.postForEntity(
                    "/auth/login", new LoginRequest(username, "wrong-password"), String.class);
            assertThat(failed.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        ResponseEntity<String> locked = restTemplate.postForEntity(
                "/auth/login", new LoginRequest(username, "wrong-password"), String.class);
        assertThat(locked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        ResponseEntity<String> lockedEvenWithCorrectPassword = restTemplate.postForEntity(
                "/auth/login", new LoginRequest(username, "password123"), String.class);
        assertThat(lockedEvenWithCorrectPassword.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    private void register(String username, Role role) {
        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/auth/register", new RegisterRequest(username, "password123", role), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private TokenPairResponse login(String username) {
        ResponseEntity<TokenPairResponse> response = restTemplate.postForEntity(
                "/auth/login", new LoginRequest(username, "password123"), TokenPairResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private HttpEntity<Void> authorized(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(headers);
    }
}
