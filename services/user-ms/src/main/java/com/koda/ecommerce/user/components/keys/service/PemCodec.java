package com.koda.ecommerce.user.components.keys.service;

import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class PemCodec {

    private static final String PRIVATE_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PRIVATE_FOOTER = "-----END PRIVATE KEY-----";
    private static final String PUBLIC_HEADER = "-----BEGIN PUBLIC KEY-----";
    private static final String PUBLIC_FOOTER = "-----END PUBLIC KEY-----";

    private PemCodec() {
    }

    static String encodePrivate(PrivateKey key) {
        return encode(PRIVATE_HEADER, PRIVATE_FOOTER, key.getEncoded());
    }

    static String encodePublic(PublicKey key) {
        return encode(PUBLIC_HEADER, PUBLIC_FOOTER, key.getEncoded());
    }

    public static PrivateKey decodePrivate(String pem) {
        try {
            byte[] der = decode(pem, PRIVATE_HEADER, PRIVATE_FOOTER);
            return KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decode RSA private key", ex);
        }
    }

    public static PublicKey decodePublic(String pem) {
        try {
            byte[] der = decode(pem, PUBLIC_HEADER, PUBLIC_FOOTER);
            return KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decode RSA public key", ex);
        }
    }

    private static String encode(String header, String footer, byte[] der) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        return header + "\n" + base64 + "\n" + footer;
    }

    private static byte[] decode(String pem, String header, String footer) {
        String body = pem.replace(header, "")
                .replace(footer, "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }
}