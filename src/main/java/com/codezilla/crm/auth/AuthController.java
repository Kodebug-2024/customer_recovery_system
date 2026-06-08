package com.codezilla.crm.auth;

import com.codezilla.crm.security.JwtService;
import com.codezilla.crm.tenant.Tenant;
import com.codezilla.crm.tenant.TenantRepository;
import com.codezilla.crm.user.Roles;
import com.codezilla.crm.user.User;
import com.codezilla.crm.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository users;
    private final TenantRepository tenants;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final EmailVerificationService verifications;
    private final TwoFactorService twoFactor;

    private final int maxFailedLogins;
    private final Duration lockDuration;
    private final boolean requireEmailVerification;

    public AuthController(UserRepository users, TenantRepository tenants,
                          PasswordEncoder encoder, JwtService jwt,
                          EmailVerificationService verifications,
                          TwoFactorService twoFactor,
                          @Value("${app.security.lockout.max-failed:5}") int maxFailedLogins,
                          @Value("${app.security.lockout.duration-minutes:15}") long lockMinutes,
                          @Value("${app.security.email-verification.required:false}") boolean requireEmailVerification) {
        this.users = users;
        this.tenants = tenants;
        this.encoder = encoder;
        this.jwt = jwt;
        this.verifications = verifications;
        this.twoFactor = twoFactor;
        this.maxFailedLogins = maxFailedLogins;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
        this.requireEmailVerification = requireEmailVerification;
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password, String totpCode) {}

    public record RegisterRequest(
            @NotBlank String businessName,
            String industry,
            String name,
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8) String password) {}

    public record VerifyResponse(boolean ok, String message) {}

    @Transactional
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        User user = users.findByEmail(req.email() == null ? null : req.email().toLowerCase()).orElse(null);
        if (user == null) {
            // Constant-ish response; do not reveal whether email exists.
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            return ResponseEntity.status(423).body(Map.of(
                    "error", "Account temporarily locked due to repeated failed logins",
                    "unlockAt", user.getLockedUntil().toString()));
        }
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            user.setFailedLoginCount(user.getFailedLoginCount() + 1);
            if (user.getFailedLoginCount() >= maxFailedLogins) {
                user.setLockedUntil(Instant.now().plus(lockDuration));
                user.setFailedLoginCount(0);
            }
            users.save(user);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
        if (!user.isEnabled()) {
            return ResponseEntity.status(403).body(Map.of("error", "Account disabled"));
        }
        if (requireEmailVerification && user.getEmailVerifiedAt() == null) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Email not verified. Check your inbox for the confirmation link."));
        }
        // 2FA challenge: if enabled, the request must include the current TOTP code.
        if (user.isTotpEnabled()) {
            if (req.totpCode() == null || req.totpCode().isBlank()) {
                return ResponseEntity.status(401).body(Map.of(
                        "error", "Two-factor code required",
                        "twoFactorRequired", true));
            }
            if (!twoFactor.verifyLoginCode(user, req.totpCode())) {
                user.setFailedLoginCount(user.getFailedLoginCount() + 1);
                if (user.getFailedLoginCount() >= maxFailedLogins) {
                    user.setLockedUntil(Instant.now().plus(lockDuration));
                    user.setFailedLoginCount(0);
                }
                users.save(user);
                return ResponseEntity.status(401).body(Map.of("error", "Invalid two-factor code"));
            }
        }
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        users.save(user);
        return ResponseEntity.ok(tokenResponse(user));
    }

    @Transactional
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        String email = req.email().toLowerCase().trim();
        if (users.existsByEmail(email)) {
            return ResponseEntity.status(409).body(Map.of("error", "Email already registered"));
        }

        Tenant tenant = new Tenant();
        tenant.setName(req.businessName().trim());
        if (req.industry() != null && !req.industry().isBlank()) {
            tenant.setIndustry(req.industry().trim());
        }
        tenant.setApiKey("tenant-" + UUID.randomUUID());
        tenants.save(tenant);

        User user = new User();
        user.setTenantId(tenant.getId());
        user.setName(req.name() == null || req.name().isBlank() ? null : req.name().trim());
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(req.password()));
        user.setRole(Roles.OWNER);
        user.setEnabled(true);
        user.setLastLoginAt(Instant.now());
        users.save(user);

        // Send verification email asynchronously of the response (best-effort).
        verifications.issueAndSend(user);

        return ResponseEntity.status(201).body(tokenResponse(user));
    }

    /** Verifies the user from a token in the email link. */
    @PostMapping("/verify-email")
    public ResponseEntity<VerifyResponse> verifyEmail(@RequestParam("token") String token) {
        Optional<User> verified = verifications.consume(token);
        if (verified.isEmpty()) {
            return ResponseEntity.status(400).body(new VerifyResponse(false, "Invalid or expired token"));
        }
        return ResponseEntity.ok(new VerifyResponse(true, "Email verified"));
    }

    /** Issue a fresh verification email if the user isn't already verified. */
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").toLowerCase().trim();
        // Always 202 regardless of existence to avoid enumeration leakage.
        users.findByEmail(email).ifPresent(u -> {
            if (u.getEmailVerifiedAt() == null) verifications.issueAndSend(u);
        });
        return ResponseEntity.accepted().body(Map.of("ok", true));
    }

    private Map<String, Object> tokenResponse(User user) {
        String token = jwt.issue(user.getId(), user.getTenantId(), user.getEmail(), user.getRole());
        return Map.of(
                "token", token,
                "user", Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "role", user.getRole(),
                        "tenantId", user.getTenantId(),
                        "emailVerified", user.getEmailVerifiedAt() != null));
    }
}
