package com.codezilla.crm.tenant;

import java.util.UUID;

/**
 * Holds the active tenant for the current request thread.
 * Populated by JwtAuthFilter (admin requests) or WebhookApiKeyFilter (webhook requests).
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID tenantId) {
        CURRENT.set(tenantId);
    }

    public static UUID get() {
        return CURRENT.get();
    }

    public static UUID require() {
        UUID id = CURRENT.get();
        if (id == null) {
            throw new IllegalStateException("No tenant bound to current thread");
        }
        return id;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
