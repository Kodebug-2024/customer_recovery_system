package com.codezilla.crm.webhook.outbound;

import com.codezilla.crm.security.SecretCipher;
import com.codezilla.crm.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * CRUD for outbound webhook subscriptions. Each tenant manages their own.
 * The shared secret is generated server-side at create-time and returned
 * exactly ONCE — subsequent reads expose only the configured-or-not flag.
 */
@RestController
@RequestMapping("/api/webhook-subscriptions")
@PreAuthorize("hasRole('ADMIN')")
public class WebhookSubscriptionController {

    private static final SecureRandom RNG = new SecureRandom();

    private final WebhookSubscriptionRepository repo;
    private final SecretCipher cipher;

    public WebhookSubscriptionController(WebhookSubscriptionRepository repo, SecretCipher cipher) {
        this.repo = repo;
        this.cipher = cipher;
    }

    public record SubView(UUID id, String targetUrl, List<String> events, boolean enabled,
                          int failureCount, Instant lastSuccessAt, Instant lastFailureAt,
                          String lastError, Instant createdAt) {
        static SubView from(WebhookSubscription s) {
            return new SubView(s.getId(), s.getTargetUrl(),
                    java.util.Arrays.asList(s.getEvents().split(",")),
                    s.isEnabled(), s.getFailureCount(),
                    s.getLastSuccessAt(), s.getLastFailureAt(), s.getLastError(),
                    s.getCreatedAt());
        }
    }

    public record CreateRequest(@NotBlank String targetUrl, @NotBlank String events) {}
    public record CreateResponse(SubView subscription, String signingSecret) {}
    public record UpdateRequest(String targetUrl, String events, Boolean enabled) {}

    @GetMapping
    public List<SubView> list() {
        return repo.findAllByTenantIdOrderByCreatedAtDesc(TenantContext.require())
                .stream().map(SubView::from).toList();
    }

    @PostMapping
    @Transactional
    public CreateResponse create(@Valid @RequestBody CreateRequest req) {
        byte[] raw = new byte[32];
        RNG.nextBytes(raw);
        String secret = "whsec_" + Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        WebhookSubscription s = new WebhookSubscription();
        s.setTargetUrl(req.targetUrl().trim());
        s.setEvents(req.events().trim().toLowerCase());
        s.setSecretEnc(cipher.encrypt(secret));
        s.setEnabled(true);
        repo.save(s);
        return new CreateResponse(SubView.from(s), secret);
    }

    @PutMapping("/{id}")
    @Transactional
    public SubView update(@PathVariable UUID id, @RequestBody UpdateRequest req) {
        WebhookSubscription s = mine(id);
        if (req.targetUrl() != null && !req.targetUrl().isBlank()) s.setTargetUrl(req.targetUrl().trim());
        if (req.events() != null && !req.events().isBlank()) s.setEvents(req.events().trim().toLowerCase());
        if (req.enabled() != null) {
            s.setEnabled(req.enabled());
            if (req.enabled()) {
                // Re-enabling clears the fail counter so it gets a fresh start.
                s.setFailureCount(0);
                s.setLastError(null);
            }
        }
        return SubView.from(s);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        repo.delete(mine(id));
        return ResponseEntity.noContent().build();
    }

    private WebhookSubscription mine(UUID id) {
        WebhookSubscription s = repo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (!s.getTenantId().equals(TenantContext.require())) throw new ResponseStatusException(NOT_FOUND);
        return s;
    }
}
