package com.codezilla.crm.auth;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * Self-service TOTP enrollment. The user must be already authenticated;
 * enrollment is a 2-step flow: begin → confirm with code from the app.
 */
@RestController
@RequestMapping("/api/2fa")
public class TwoFactorController {

    private final TwoFactorService twoFactor;
    private final com.codezilla.crm.user.UserRepository users;

    public TwoFactorController(TwoFactorService twoFactor, com.codezilla.crm.user.UserRepository users) {
        this.twoFactor = twoFactor;
        this.users = users;
    }

    public record EnrollResponse(String secret, String qrDataUrl) {}
    public record ConfirmRequest(@NotBlank String code) {}
    public record StatusResponse(boolean enabled) {}

    @GetMapping("/status")
    public StatusResponse status(@AuthenticationPrincipal UUID userId) {
        if (userId == null) throw new ResponseStatusException(UNAUTHORIZED);
        var u = users.findById(userId).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        return new StatusResponse(u.isTotpEnabled());
    }

    @PostMapping("/enroll")
    public EnrollResponse enroll(@AuthenticationPrincipal UUID userId) {
        if (userId == null) throw new ResponseStatusException(UNAUTHORIZED);
        var data = twoFactor.beginEnrollment(userId);
        return new EnrollResponse(data.secret(), data.qrDataUrl());
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@AuthenticationPrincipal UUID userId, @RequestBody ConfirmRequest body) {
        if (userId == null) throw new ResponseStatusException(UNAUTHORIZED);
        boolean ok = twoFactor.confirmEnrollment(userId, body.code());
        if (!ok) return ResponseEntity.status(BAD_REQUEST).body(Map.of("error", "Invalid code"));
        return ResponseEntity.ok(Map.of("enabled", true));
    }

    @PostMapping("/disable")
    public ResponseEntity<?> disable(@AuthenticationPrincipal UUID userId, @RequestBody ConfirmRequest body) {
        if (userId == null) throw new ResponseStatusException(UNAUTHORIZED);
        var u = users.findById(userId).orElseThrow();
        if (!u.isTotpEnabled()) return ResponseEntity.ok(Map.of("enabled", false));
        // Require a valid current code to disable, so a stolen session cannot turn off 2FA.
        if (!twoFactor.verifyLoginCode(u, body.code())) {
            return ResponseEntity.status(BAD_REQUEST).body(Map.of("error", "Invalid code"));
        }
        twoFactor.disable(userId);
        return ResponseEntity.ok(Map.of("enabled", false));
    }
}
