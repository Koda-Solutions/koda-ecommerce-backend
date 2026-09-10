package com.koda.ecommerce.user.components.auth.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class CookieService {

    public static final String ACCESS_COOKIE = "CUSTOMER_AUTH_TOKEN";
    public static final String REFRESH_COOKIE = "CUSTOMER_REFRESH_TOKEN";

    @Value("${auth.access-token-ttl-seconds}")
    private long accessTokenTtlSeconds;

    @Value("${auth.refresh-token-ttl-days}")
    private long refreshTokenTtlDays;

    @Value("${auth.cookie-secure}")
    private boolean cookieSecure;

    public ResponseCookie accessCookie(String token) {
        return build(ACCESS_COOKIE, token, Duration.ofSeconds(accessTokenTtlSeconds));
    }

    public ResponseCookie refreshCookie(String token) {
        return build(REFRESH_COOKIE, token, Duration.ofDays(refreshTokenTtlDays));
    }

    public ResponseCookie expiredCookie(String name) {
        return build(name, "", Duration.ZERO);
    }

    private ResponseCookie build(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    public static HttpHeaders httpOnlyHeaders(ResponseCookie... cookies) {
        HttpHeaders headers = new HttpHeaders();
        for (ResponseCookie cookie : cookies) {
            headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return headers;
    }

    public static String read(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}