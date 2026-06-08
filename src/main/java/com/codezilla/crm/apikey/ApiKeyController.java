package com.codezilla.crm.apikey;

import com.codezilla.crm.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * CRUD for personal API keys. Each key is scoped to the user who created it
 * and inherits that user's role (so a VIEWER's key can only read). The
 * cleartext is shown exactly once at create-time; thereafter only a suffix.
 */
@RestController
@RequestMapping("/api/api-keys")
public class ApiKeyController {

    private static final SecureRandom RNG = new SecureRandom();

    private final ApiKeyRepository repo;

    public ApiKeyController(ApiKeyRepository repo) {
        this.repo = repo;
    }

    public record KeyView(UUID id, String name, String keySuffix,
                          Instant lastUsedAt, Instant expiresAt,
                          Instant createdAt, Instant revokedAt) {
        static KeyView from(ApiKey k) {
            return new KeyView(k.getId(), k.getName(), k.getKeySuffix(),
                    k.getLastUsedAt(), k.getExpiresAt(), k.getCreatedAt(), k.getRevokedAt());
        }
    }
    public record CreateRequest(@NotBlank String name, Integer expiresInDays) {}
    public record CreateResponse(KeyView key, String secret) {}

    @GetMapping
    public List<KeyView> list(@AuthenticationPrincipal UUID userId) {
        if (userId == null) throw new ResponseStatusException(UNAUTHORIZED);
        return repo.findAllByTenantIdAndUserIdOrderByCreatedAtDesc(TenantContext.require(), userId)
                .stream().map(KeyView::from).toList();
    }

    @PostMapping
    @Transactional
    public CreateResponse create(@AuthenticationPrincipal UUID userId,
                                 @Valid @RequestBody CreateRequest req) {
        if (userId == null) throw new ResponseStatusException(UNAUTHORIZED);
        byte[] raw = new byte[32];
        RNG.nextBytes(raw);
        String secret = "crm_" + Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        ApiKey k = new ApiKey();
        k.setUserId(userId);
        k.setName(req.name().trim());
        k.setKeyHash(ApiKeyAuthFilter.sha256Hex(secret));
        k.setKeySuffix(secret.substring(secret.length() - 4));
        if (req.expiresInDays() != null && req.expiresInDays() > 0) {
            k.setExpiresAt(Instant.now().plusSeconds(req.expiresInDays() * 86_400L));
        }
        repo.save(k);
        return new CreateResponse(KeyView.from(k), secret);
    }

    @PostMapping("/{id}/revoke")
    @Transactional
    public KeyView revoke(@PathVariable UUID id, @AuthenticationPrincipal UUID userId) {
        ApiKey k = repo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (!k.getTenantId().equals(TenantContext.require())) throw new ResponseStatusException(NOT_FOUND);
        // ADMINs can revoke any tenant key; otherwise only your own.
        // We can't easily check role here without injecting it; safer rule: only owner can revoke,
        // and ADMIN goes through a separate endpoint if needed.
        if (!k.getUserId().equals(userId)) throw new ResponseStatusException(NOT_FOUND);
        if (k.getRevokedAt() == null) k.setRevokedAt(Instant.now());
        return KeyView.from(k);
    }
}
