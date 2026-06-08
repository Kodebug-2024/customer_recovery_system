package com.codezilla.crm.auth;

import com.codezilla.crm.integration.EmailClient;
import com.codezilla.crm.user.User;
import com.codezilla.crm.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Forgot-password flow:
 * 1. {@link #issueAndSend(String)} — generate a random token, store its SHA-256 hash, email the cleartext to the user.
 * 2. {@link #consume(String, String)} — verify the hash, expire the token, set the new password.
 *
 * Token cleartext is **never** stored. Hashing means a DB leak doesn't grant
 * password-reset capability to attackers.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RNG = new SecureRandom();

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final EmailClient email;
    private final PasswordEncoder encoder;
    private final String publicBaseUrl;
    private final Duration ttl;

    public PasswordResetService(UserRepository users,
                                PasswordResetTokenRepository tokens,
                                EmailClient email,
                                PasswordEncoder encoder,
                                @Value("${app.public-base-url:http://localhost:3000}") String publicBaseUrl,
                                @Value("${app.security.password-reset.ttl-minutes:60}") long ttlMinutes) {
        this.users = users;
        this.tokens = tokens;
        this.email = email;
        this.encoder = encoder;
        this.publicBaseUrl = publicBaseUrl;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    /** Idempotent: always succeeds, never reveals whether the email exists. */
    @Transactional
    public void issueAndSend(String emailAddress) {
        if (emailAddress == null || emailAddress.isBlank()) return;
        Optional<User> userOpt = users.findByEmail(emailAddress.toLowerCase().trim());
        if (userOpt.isEmpty()) {
            log.info("Password reset requested for unknown email; ignoring (response is identical)");
            return;
        }
        User user = userOpt.get();

        // Invalidate any prior tokens for this user so only the most recent works.
        tokens.deleteAllForUser(user.getId());

        String cleartext = newToken();
        PasswordResetToken t = new PasswordResetToken();
        t.setTokenHash(sha256Hex(cleartext));
        t.setUserId(user.getId());
        t.setExpiresAt(Instant.now().plus(ttl));
        tokens.save(t);

        String url = publicBaseUrl + "/reset-password?token=" + cleartext;
        String body = """
                Hi%s,

                Someone (hopefully you) requested a password reset for your Codezilla CRM
                account. Click the link below within %d minutes to set a new password:

                %s

                If you didn't request this, you can ignore this email — your password
                won't change.
                """.formatted(
                user.getName() == null ? "" : " " + user.getName(),
                ttl.toMinutes(),
                url);
        try {
            email.send(user.getEmail(), "Reset your password", body);
        } catch (Exception e) {
            log.warn("Failed to send password reset email to {}", user.getEmail(), e);
        }
    }

    /**
     * Verify the token and set a new password.
     * @return true if the reset succeeded; false if the token is unknown / expired / already used.
     */
    @Transactional
    public boolean consume(String cleartext, String newPassword) {
        if (cleartext == null || cleartext.isBlank()) return false;
        if (newPassword == null || newPassword.length() < 8) return false;
        String hash = sha256Hex(cleartext);
        return tokens.findById(hash).map(t -> {
            if (t.getUsedAt() != null) return false;
            if (t.getExpiresAt().isBefore(Instant.now())) return false;
            User u = users.findById(t.getUserId()).orElse(null);
            if (u == null) return false;
            u.setPasswordHash(encoder.encode(newPassword));
            u.setFailedLoginCount(0);
            u.setLockedUntil(null);
            t.setUsedAt(Instant.now());
            return true;
        }).orElse(false);
    }

    private static String newToken() {
        byte[] buf = new byte[32];
        RNG.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
