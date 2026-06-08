package com.codezilla.crm.lead;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {
    Page<Lead> findAllByTenantId(UUID tenantId, Pageable pageable);
    Page<Lead> findAllByTenantIdAndStatus(UUID tenantId, LeadStatus status, Pageable pageable);
    Page<Lead> findAllByTenantIdAndSource(UUID tenantId, String source, Pageable pageable);
    Page<Lead> findAllByTenantIdAndAssignedToUserId(UUID tenantId, UUID assignedToUserId, Pageable pageable);
    List<Lead> findAllByTenantId(UUID tenantId);
    long countByTenantIdAndStatus(UUID tenantId, LeadStatus status);
}
