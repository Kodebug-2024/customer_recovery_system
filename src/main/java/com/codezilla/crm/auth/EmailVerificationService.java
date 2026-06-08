package com.codezilla.crm.auth;

import com.codezilla.crm.integration.EmailClient;
import com.codezilla.crm.user.User;
import com.codezilla.crm.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Issues and consumes email-verification tokens. Tokens are random 32 bytes,
 * base64url-encoded, stored on the user row alongside an expiry. Verification
 * email goes through the configured EmailClient (stub in dev; SES SMTP or any
 * SMTP server in prod via Spring Mail).
 */
@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final SecureRandom RNG = new SecureRandom();

    private final UserRepository users;
    private final EmailClient email;
    private final String appBaseUrl;
    private final Duration ttl;

    public EmailVerificationService(UserRepository users, EmailClient email,
                                    @Value("${app.public-base-url:http://localhost:3000}") String appBaseUrl,
                                    @Value("${app.security.email-verification.ttl-hours:48}") long ttlHours) {
        this.users = users;
        this.email = email;
        this.appBaseUrl = appBaseUrl;
        this.ttl = Duration.ofHours(ttlHours);
    }

    /** Issue a fresh token, persist on the user, and send the verification email. */
    @Transactional
    public void issueAndSend(User user) {
        String token = newToken();
        user.setEmailVerificationToken(token);
        user.setEmailVerificationExpiresAt(Instant.now().plus(ttl));
        users.save(user);

        String url = appBaseUrl + "/verify-email?token=" + token;
        String body = """
                Hi%s,

                Confirm your email to activate your Codezilla CRM workspace:

                %s

                This link expires in %d hours.

                If you didn't sign up, ignore this email.
                """.formatted(
                user.getName() == null ? "" : " " + user.getName(),
                url,
                ttl.toHours());
        try {
            email.send(user.getEmail(), "Confirm your email", body);
        } catch (Exception e) {
            log.warn("Failed to send verification email to {}", user.getEmail(), e);
        }
    }

    /** Mark the user verified if the token matches and hasn't expired. */
    @Transactional
    public Optional<User> consume(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return users.findByEmailVerificationToken(token).map(u -> {
            if (u.getEmailVerificationExpiresAt() == null
                    || u.getEmailVerificationExpiresAt().isBefore(Instant.now())) {
                return null;
            }
            u.setEmailVerifiedAt(Instant.now());
            u.setEmailVerificationToken(null);
            u.setEmailVerificationExpiresAt(null);
            return u;
        }).filter(java.util.Objects::nonNull);
    }

    private static String newToken() {
        byte[] buf = new byte[32];
        RNG.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
