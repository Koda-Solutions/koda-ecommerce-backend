package com.koda.ecommerce.user.components.keys.service;

import com.koda.ecommerce.user.model.RefreshToken;
import com.koda.ecommerce.user.model.SigningKey;
import com.koda.ecommerce.user.model.SubjectType;
import com.koda.ecommerce.user.repository.RefreshTokenRepository;
import com.koda.ecommerce.user.repository.SigningKeyRepository;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KeyRotationService {

    private final SigningKeyRepository signingKeyRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public SigningKey activeKey() {
        return signingKeyRepository.findFirstByActiveTrueOrderByIdDesc()
                .orElseGet(this::createFirstKey);
    }

    @Transactional
    public synchronized int rotateFor(SubjectType subjectType, Long subjectId) {
        SigningKey next = createNextKey();
        LocalDateTime now = LocalDateTime.now();
        List<RefreshToken> tokens = refreshTokenRepository
                .findAllBySubjectTypeAndSubjectIdAndRevokedAtIsNull(subjectType, subjectId);
        tokens.forEach(token -> {
            token.setRevokedAt(now);
            refreshTokenRepository.save(token);
        });
        return next.getKeyVersion();
    }

    @Transactional
    protected SigningKey createNextKey() {
        int nextVersion = activeVersion() + 1;
        SigningKey key = generate(nextVersion);
        signingKeyRepository.findAllByActiveTrue().forEach(old -> {
            old.setActive(false);
            signingKeyRepository.save(old);
        });
        return signingKeyRepository.save(key);
    }

    private SigningKey createFirstKey() {
        return signingKeyRepository.save(generate(1));
    }

    @Transactional(readOnly = true)
    protected int activeVersion() {
        return signingKeyRepository.findFirstByActiveTrueOrderByIdDesc()
                .map(SigningKey::getKeyVersion)
                .orElse(0);
    }

    @Transactional
    public List<Map<String, Object>> activeJwks() {
        List<SigningKey> keys = signingKeyRepository.findAllByActiveTrue();
        List<Map<String, Object>> jwks = new ArrayList<>();
        for (SigningKey key : keys) {
            RSAPublicKey publicKey = (RSAPublicKey) PemCodec.decodePublic(key.getPublicKey());
            Map<String, Object> jwk = new HashMap<>();
            jwk.put("kty", "RSA");
            jwk.put("use", "sig");
            jwk.put("alg", "RS256");
            jwk.put("kid", String.valueOf(key.getKeyVersion()));
            jwk.put("n", unsignedBase64Url(publicKey.getModulus()));
            jwk.put("e", unsignedBase64Url(publicKey.getPublicExponent()));
            jwks.add(jwk);
        }
        if (jwks.isEmpty()) {
            return List.of(activeKeyJwk());
        }
        return jwks;
    }

    private Map<String, Object> activeKeyJwk() {
        SigningKey key = createFirstKey();
        RSAPublicKey publicKey = (RSAPublicKey) PemCodec.decodePublic(key.getPublicKey());
        Map<String, Object> jwk = new HashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("kid", String.valueOf(key.getKeyVersion()));
        jwk.put("n", unsignedBase64Url(publicKey.getModulus()));
        jwk.put("e", unsignedBase64Url(publicKey.getPublicExponent()));
        return jwk;
    }

    private static String unsignedBase64Url(BigInteger value) {
        byte[] raw = value.toByteArray();
        if (raw.length > 1 && raw[0] == 0) {
            byte[] withoutSign = new byte[raw.length - 1];
            System.arraycopy(raw, 1, withoutSign, 0, withoutSign.length);
            raw = withoutSign;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    private SigningKey generate(int version) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            SigningKey key = new SigningKey();
            key.setKeyVersion(version);
            key.setActive(true);
            key.setPrivateKey(PemCodec.encodePrivate((RSAPrivateKey) pair.getPrivate()));
            key.setPublicKey(PemCodec.encodePublic((RSAPublicKey) pair.getPublic()));
            key.setCreatedAt(LocalDateTime.now());
            return key;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate RSA signing key", ex);
        }
    }
}