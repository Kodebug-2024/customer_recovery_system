package com.codezilla.crm.lead;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LeadNoteRepository extends JpaRepository<LeadNote, UUID> {
    List<LeadNote> findAllByTenantIdAndLeadIdOrderByCreatedAtDesc(UUID tenantId, UUID leadId);
}
