package com.codezilla.crm.template;

import com.codezilla.crm.tenant.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "message_templates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "name"}))
@Getter
@Setter
public class MessageTemplate {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "name", length = 120, nullable = false)
    private String name;

    @Column(name = "channel", length = 32, nullable = false)
    private String channel;

    @Column(name = "event", length = 32, nullable = false)
    private String event;

    @Column(name = "subject", length = 255)
    private String subject;

    @Column(name = "body", columnDefinition = "text", nullable = false)
    private String body;

    @Column(name = "is_default", nullable = false)
    private boolean defaultForEvent;

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
