package com.koda.ecommerce.user.components.auth.controller;

import com.koda.ecommerce.user.components.auth.dto.login.LoginRequestDTO;
import com.koda.ecommerce.user.components.auth.dto.me.CustomerResponseDTO;
import com.koda.ecommerce.user.components.auth.dto.password.PasswordRequestDTO;
import com.koda.ecommerce.user.components.auth.dto.register.RegisterRequestDTO;
import com.koda.ecommerce.user.components.auth.service.AuthService;
import com.koda.ecommerce.user.components.auth.service.CookieService;
import com.koda.ecommerce.user.components.auth.service.PrincipalResolver;
import com.koda.ecommerce.user.components.auth.service.RegisterResult;
import com.koda.ecommerce.user.components.keys.service.KeyRotationService;
import com.koda.ecommerce.user.components.tokens.RefreshRotation;
import com.koda.ecommerce.user.components.tokens.TokenPair;
import com.koda.ecommerce.user.model.Customer;
import com.koda.ecommerce.user.utils.ReturnObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieService cookieService;
    private final PrincipalResolver principalResolver;
    private final KeyRotationService keyRotationService;

    @PostMapping("/register")
    public ResponseEntity<ReturnObject<CustomerResponseDTO>> register(
            @Valid @RequestBody RegisterRequestDTO request) {
        RegisterResult result = authService.register(request);
        HttpHeaders headers = CookieService.httpOnlyHeaders(
                cookieService.accessCookie(result.tokens().accessToken()),
                cookieService.refreshCookie(result.tokens().refreshToken()));
        return authorized(CustomerResponseDTO.from(result.customer()), headers);
    }

    @PostMapping("/login")
    public ResponseEntity<ReturnObject<CustomerResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletRequest http) {
        TokenPair tokens = authService.login(request, clientIp(http));
        HttpHeaders headers = CookieService.httpOnlyHeaders(
                cookieService.accessCookie(tokens.accessToken()),
                cookieService.refreshCookie(tokens.refreshToken()));
        return authorized(null, headers);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ReturnObject<CustomerResponseDTO>> refresh(HttpServletRequest request) {
        String rawRefresh = CookieService.read(request, CookieService.REFRESH_COOKIE);
        RefreshRotation rotation = authService.refresh(rawRefresh);
        HttpHeaders headers = CookieService.httpOnlyHeaders(
                cookieService.accessCookie(rotation.accessToken()),
                cookieService.refreshCookie(rotation.refreshToken()));
        return authorized(CustomerResponseDTO.from(rotation.customer()), headers);
    }

    @PostMapping("/logout")
    public ResponseEntity<ReturnObject<Void>> logout(HttpServletRequest request) {
        Customer customer = principalResolver.resolveCustomer(request);
        authService.logout(customer);
        HttpHeaders headers = CookieService.httpOnlyHeaders(
                cookieService.expiredCookie(CookieService.ACCESS_COOKIE),
                cookieService.expiredCookie(CookieService.REFRESH_COOKIE));
        ReturnObject<Void> body = ReturnObject.ok("Logged out", null);
        return ResponseEntity.ok().headers(headers).body(body);
    }

    @PostMapping("/password")
    public ResponseEntity<ReturnObject<CustomerResponseDTO>> changePassword(
            @Valid @RequestBody PasswordRequestDTO request,
            HttpServletRequest http) {
        Customer customer = principalResolver.resolveCustomer(http);
        TokenPair tokens = authService.changePassword(customer, request);
        HttpHeaders headers = CookieService.httpOnlyHeaders(
                cookieService.accessCookie(tokens.accessToken()),
                cookieService.refreshCookie(tokens.refreshToken()));
        return authorized(CustomerResponseDTO.from(customer), headers);
    }

    @GetMapping("/keys")
    public ResponseEntity<ReturnObject<Map<String, Object>>> keys() {
        Map<String, Object> jwks = Map.of("keys",
                (Object) keyRotationService.activeJwks());
        ReturnObject<Map<String, Object>> body = ReturnObject.ok("Public signing keys",
                jwks);
        return ResponseEntity.ok(body);
    }

    private <T> ResponseEntity<ReturnObject<T>> authorized(T data, HttpHeaders headers) {
        return ResponseEntity.ok()
                .headers(headers)
                .body(ReturnObject.ok("success", data));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}