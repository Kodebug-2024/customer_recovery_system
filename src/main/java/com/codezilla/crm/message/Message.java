package com.codezilla.crm.message;

import com.codezilla.crm.tenant.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter
@Setter
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Message extends TenantAwareEntity {

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 16, nullable = false)
    private MessageDirection direction;

    @Column(name = "content", columnDefinition = "text", nullable = false)
    private String content;

    @Column(name = "channel", length = 20, nullable = false)
    private String channel;
}
