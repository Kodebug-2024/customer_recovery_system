package com.codezilla.crm.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM-256 encryption for at-rest secrets (per-tenant integration tokens).
 *
 * Key source: APP_ENCRYPTION_KEY env var, base64-encoded 32 bytes.
 * For dev, a deterministic fallback derived from JWT secret is used so the app
 * still starts; do NOT rely on the fallback in production.
 *
 * Payload format: [12-byte IV] || [ciphertext+tag]. Stored as bytea.
 */
@Component
public class SecretCipher {

    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SecretKeySpec key;
    private final SecureRandom rng = new SecureRandom();

    public SecretCipher(@Value("${app.security.encryption.key:}") String base64Key,
                        @Value("${app.security.jwt.secret}") String jwtSecret) {
        byte[] raw;
        if (base64Key != null && !base64Key.isBlank()) {
            raw = Base64.getDecoder().decode(base64Key);
            if (raw.length != 32) {
                throw new IllegalStateException("APP_ENCRYPTION_KEY must decode to 32 bytes (got " + raw.length + ")");
            }
        } else {
            // Dev fallback only: derive a 32-byte key from JWT secret. Not safe for prod.
            byte[] src = jwtSecret.getBytes(StandardCharsets.UTF_8);
            raw = new byte[32];
            for (int i = 0; i < 32; i++) raw[i] = src[i % src.length];
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    public byte[] encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_LEN];
            rng.nextBytes(iv);
            Cipher c = Cipher.getInstance(TRANSFORMATION);
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = c.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[IV_LEN + ct.length];
            System.arraycopy(iv, 0, out, 0, IV_LEN);
            System.arraycopy(ct, 0, out, IV_LEN, ct.length);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Encrypt failed", e);
        }
    }

    public String decrypt(byte[] payload) {
        if (payload == null || payload.length <= IV_LEN) return null;
        try {
            byte[] iv = new byte[IV_LEN];
            System.arraycopy(payload, 0, iv, 0, IV_LEN);
            byte[] ct = new byte[payload.length - IV_LEN];
            System.arraycopy(payload, IV_LEN, ct, 0, ct.length);
            Cipher c = Cipher.getInstance(TRANSFORMATION);
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(c.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decrypt failed", e);
        }
    }
}
