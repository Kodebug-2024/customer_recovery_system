package com.codezilla.crm.apikey;

import com.codezilla.crm.tenant.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_keys")
@Getter
@Setter
public class ApiKey {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "name", length = 120, nullable = false)
    private String name;

    @Column(name = "key_hash", length = 128, nullable = false, unique = true, updatable = false)
    private String keyHash;

    @Column(name = "key_suffix", length = 8, nullable = false, updatable = false)
    private String keySuffix;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (tenantId == null) tenantId = TenantContext.require();
        if (createdAt == null) createdAt = Instant.now();
    }

    public boolean isUsable() {
        return revokedAt == null
                && (expiresAt == null || expiresAt.isAfter(Instant.now()));
    }
}
