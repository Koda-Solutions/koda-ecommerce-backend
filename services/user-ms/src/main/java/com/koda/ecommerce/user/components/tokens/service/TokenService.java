package com.koda.ecommerce.user.components.tokens.service;

import com.koda.ecommerce.user.components.keys.service.KeyRotationService;
import com.koda.ecommerce.user.components.keys.service.PemCodec;
import com.koda.ecommerce.user.components.tokens.RefreshRotation;
import com.koda.ecommerce.user.components.tokens.TokenPair;
import com.koda.ecommerce.user.model.Customer;
import com.koda.ecommerce.user.model.RefreshToken;
import com.koda.ecommerce.user.model.SigningKey;
import com.koda.ecommerce.user.model.SubjectType;
import com.koda.ecommerce.user.repository.CustomerRepository;
import com.koda.ecommerce.user.repository.RefreshTokenRepository;
import com.koda.ecommerce.user.repository.SigningKeyRepository;
import com.koda.ecommerce.user.utils.ApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.koda.ecommerce.user.utils.ErrorCodes.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
public class TokenService {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ACCESS = "access";
    private static final String CLAIM_VERSION = "ver";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final KeyRotationService keyRotationService;
    private final SigningKeyRepository signingKeyRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CustomerRepository customerRepository;

    @Value("${auth.issuer}")
    private String issuer;

    @Value("${auth.access-token-ttl-seconds}")
    private long accessTokenTtlSeconds;

    @Value("${auth.refresh-token-ttl-days}")
    private long refreshTokenTtlDays;

    @Transactional
    public TokenPair issueForCustomer(Customer customer) {
        String accessToken = issueAccessToken(customer);
        String refreshToken = persistRefreshToken(customer);
        return new TokenPair(accessToken, refreshToken);
    }

    public String issueAccessToken(Customer customer) {
        SigningKey key = latestActiveKey();
        PrivateKey privateKey = PemCodec.decodePrivate(key.getPrivateKey());
        Date now = new Date();
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(customer.getId()))
                .claim(CLAIM_TYPE, CLAIM_ACCESS)
                .claim(CLAIM_VERSION, customer.getKeyVersion())
                .claim("roles", List.of())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenTtlSeconds * 1000))
                .header().keyId(String.valueOf(key.getKeyVersion())).and()
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public Claims verifyAccess(String token) {
        List<SigningKey> keys = signingKeyRepository.findAllByActiveTrue();
        for (SigningKey key : keys) {
            try {
                PublicKey publicKey = PemCodec.decodePublic(key.getPublicKey());
                Claims claims = Jwts.parser()
                        .verifyWith(publicKey)
                        .requireIssuer(issuer)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
                if (!CLAIM_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
                    throw new ApiException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED,
                            "Token is not an access token");
                }
                return claims;
            } catch (ApiException ex) {
                throw ex;
            } catch (Exception ignored) {
                // try the next active key
            }
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED, "Token is invalid");
    }

    @Transactional
    public RefreshRotation rotateCustomerRefresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED,
                    "Refresh token is missing");
        }
        String hash = hash(rawRefreshToken);
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED,
                        "Refresh token is unknown"));

        if (current.getRevokedAt() != null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED,
                    "Refresh token was already used");
        }
        if (current.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED,
                    "Refresh token has expired");
        }
        if (!SubjectType.CUSTOMER.equals(current.getSubjectType())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED,
                    "Refresh token is not a customer token");
        }

        Customer customer = customerRepository.findById(current.getSubjectId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED,
                        "Customer no longer exists"));

        String nextRaw = rawRefreshToken();
        String nextHash = hash(nextRaw);
        LocalDateTime now = LocalDateTime.now();
        current.setRevokedAt(now);
        current.setReplacedBy(nextHash);
        refreshTokenRepository.save(current);

        RefreshToken next = new RefreshToken();
        next.setSubjectType(SubjectType.CUSTOMER);
        next.setSubjectId(customer.getId());
        next.setTokenHash(nextHash);
        next.setExpiresAt(now.plusDays(refreshTokenTtlDays));
        next.setCreatedAt(now);
        refreshTokenRepository.save(next);

        return new RefreshRotation(issueAccessToken(customer), nextRaw, customer);
    }

    private String persistRefreshToken(Customer customer) {
        String raw = rawRefreshToken();
        RefreshToken token = new RefreshToken();
        token.setSubjectType(SubjectType.CUSTOMER);
        token.setSubjectId(customer.getId());
        token.setTokenHash(hash(raw));
        token.setExpiresAt(LocalDateTime.now().plusDays(refreshTokenTtlDays));
        token.setCreatedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);
        return raw;
    }

    private SigningKey latestActiveKey() {
        return keyRotationService.activeKey();
    }

    private String rawRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}