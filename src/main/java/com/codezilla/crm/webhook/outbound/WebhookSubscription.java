package com.codezilla.crm.webhook.outbound;

import com.codezilla.crm.tenant.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_subscriptions")
@Getter
@Setter
public class WebhookSubscription {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "target_url", length = 2048, nullable = false)
    private String targetUrl;

    /** AES-GCM encrypted shared secret used to HMAC-sign each delivery. */
    @Column(name = "secret_enc", nullable = false)
    private byte[] secretEnc;

    /** Comma-separated event names (e.g. "lead.created,lead.status_changed"). */
    @Column(name = "events", columnDefinition = "text", nullable = false)
    private String events;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @Column(name = "last_failure_at")
    private Instant lastFailureAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (tenantId == null) tenantId = TenantContext.require();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    /** True if this subscription is interested in the given event name. */
    public boolean handles(String eventName) {
        if (events == null || events.isBlank()) return false;
        for (String e : events.split(",")) {
            if (e.trim().equalsIgnoreCase(eventName)) return true;
            // Wildcard: "lead.*" matches "lead.created"
            if (e.endsWith(".*")) {
                String prefix = e.substring(0, e.length() - 2);
                if (eventName != null && eventName.startsWith(prefix + ".")) return true;
            }
        }
        return false;
    }
}
