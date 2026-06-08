package com.codezilla.crm.lead;

import com.codezilla.crm.tenant.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@Entity
@Table(name = "leads")
@Getter
@Setter
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@SQLRestriction("deleted_at IS NULL")
public class Lead extends TenantAwareEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "source", length = 50, nullable = false)
    private String source;

    @Column(name = "message", columnDefinition = "text")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private LeadStatus status = LeadStatus.NEW;

    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
