package com.codezilla.crm.ai.faq;

import com.codezilla.crm.tenant.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "faqs")
@Getter
@Setter
public class FaqEntry {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "pattern", nullable = false, columnDefinition = "text")
    private String pattern;

    @Column(name = "reply", nullable = false, columnDefinition = "text")
    private String reply;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "hit_count", nullable = false)
    private long hitCount;

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
}
