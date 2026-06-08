package com.codezilla.crm.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    Page<AuditEvent> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<AuditEvent> findAllByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
            UUID tenantId, String entityType, UUID entityId, Pageable pageable);
}
