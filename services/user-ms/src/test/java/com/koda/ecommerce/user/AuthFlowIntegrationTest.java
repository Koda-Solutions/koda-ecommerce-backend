package com.koda.ecommerce.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthFlowIntegrationTest {

    private static final String BASE = "";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void useApacheClient() {
        rest.getRestTemplate().setRequestFactory(
                new HttpComponentsClientHttpRequestFactory());
    }

    @Test
    void registerThenReadProfileWithCookie() throws Exception {
        String email = uniqueEmail();
        ResponseEntity<String> registered = register(email, "profile-test");
        HttpHeaders headers = registered.getHeaders();

        assertThat(headers.get("Set-Cookie")).isNotNull();
        String access = cookieValue(headers, "CUSTOMER_AUTH_TOKEN");
        String refresh = cookieValue(headers, "CUSTOMER_REFRESH_TOKEN");
        assertThat(access).isNotBlank();
        assertThat(refresh).isNotBlank();

        ResponseEntity<String> me = get("/customer/me", cookies(access, refresh));
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(me.getBody());
        assertThat(body.path("status").asBoolean()).isTrue();
        assertThat(body.at("/data/email").asText()).isEqualTo(email);
        assertThat(body.at("/data/fullName").asText()).isEqualTo("profile-test");
    }

    @Test
    void wrongPasswordsAreRecordedThenRateLimited() {
        String identifier = uniqueEmail();

        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> failed = login(identifier, "wrong-password");
            assertThat(failed.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        ResponseEntity<String> limited = login(identifier, "wrong-password");
        assertThat(limited.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void refreshRotatesAndLogoutKillsTokens() {
        HttpHeaders headers = register(uniqueEmail(), "rotate-test").getHeaders();
        String access0 = cookieValue(headers, "CUSTOMER_AUTH_TOKEN");
        String refresh0 = cookieValue(headers, "CUSTOMER_REFRESH_TOKEN");

        ResponseEntity<String> refreshed = refresh(refresh0);
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        String access1 = cookieValue(refreshed.getHeaders(), "CUSTOMER_AUTH_TOKEN");
        String refresh1 = cookieValue(refreshed.getHeaders(), "CUSTOMER_REFRESH_TOKEN");
        assertThat(refresh1).isNotEqualTo(refresh0);
        assertThat(access1).isNotEqualTo(access0);

        ResponseEntity<String> loggedOut = post("/auth/logout", null, cookies(access1, refresh1));
        assertThat(loggedOut.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(get("/customer/me", cookies(access1, refresh1)).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(refresh(refresh1).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(refresh(refresh0).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void changePasswordRotatesKeysAndKillsOldToken() {
        HttpHeaders headers = register(uniqueEmail(), "password-test").getHeaders();
        String access0 = cookieValue(headers, "CUSTOMER_AUTH_TOKEN");
        String refresh0 = cookieValue(headers, "CUSTOMER_REFRESH_TOKEN");

        Map<String, Object> body = Map.of(
                "currentPassword", "Str0ngPassword!",
                "newPassword", "AnotherStr0ng!");
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        requestHeaders.addAll(cookies(access0, refresh0));
        ResponseEntity<String> changed = rest.exchange(BASE + "/auth/password",
                HttpMethod.POST, new HttpEntity<>(body, requestHeaders), String.class);
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        String access1 = cookieValue(changed.getHeaders(), "CUSTOMER_AUTH_TOKEN");

        assertThat(get("/customer/me", cookies(access0, refresh0)).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get("/customer/me", cookies(access1, null)).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<String> register(String email, String fullName) {
        Map<String, Object> body = Map.of(
                "fullName", fullName,
                "email", email,
                "password", "Str0ngPassword!",
                "language", "ar");
        return rest.postForEntity(BASE + "/auth/register", jsonEntity(body), String.class);
    }

    private ResponseEntity<String> login(String identifier, String password) {
        Map<String, Object> body = Map.of("identifier", identifier, "password", password);
        return rest.postForEntity(BASE + "/auth/login", jsonEntity(body), String.class);
    }

    private ResponseEntity<String> refresh(String rawRefresh) {
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(cookies(null, rawRefresh));
        return rest.exchange(BASE + "/auth/refresh", HttpMethod.POST,
                new HttpEntity<>(headers), String.class);
    }

    private HttpEntity<Map<String, Object>> jsonEntity(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private ResponseEntity<String> get(String path, HttpHeaders cookies) {
        return rest.exchange(BASE + path, HttpMethod.GET, new HttpEntity<>(cookies),
                String.class);
    }

    private ResponseEntity<String> post(String path, Object body, HttpHeaders cookies) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.addAll(cookies);
        return rest.exchange(BASE + path, HttpMethod.POST, new HttpEntity<>(body, headers),
                String.class);
    }

    private static HttpHeaders cookies(String access, String refresh) {
        HttpHeaders headers = new HttpHeaders();
        StringBuilder cookie = new StringBuilder();
        if (access != null) {
            cookie.append("CUSTOMER_AUTH_TOKEN=").append(access);
        }
        if (refresh != null) {
            if (!cookie.isEmpty()) {
                cookie.append("; ");
            }
            cookie.append("CUSTOMER_REFRESH_TOKEN=").append(refresh);
        }
        if (!cookie.isEmpty()) {
            headers.set(HttpHeaders.COOKIE, cookie.toString());
        }
        return headers;
    }

    private static String cookieValue(HttpHeaders headers, String name) {
        List<String> setCookies = headers.get("Set-Cookie");
        if (setCookies == null) {
            return null;
        }
        for (String cookie : setCookies) {
            if (cookie.startsWith(name + "=")) {
                return cookie.substring(name.length() + 1, cookie.indexOf(';'));
            }
        }
        return null;
    }

    private static String uniqueEmail() {
        return "test-" + UUID.randomUUID() + "@example.com";
    }
}