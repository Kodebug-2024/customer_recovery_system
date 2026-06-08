package com.codezilla.crm.lead;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {
    Page<Lead> findAllByTenantId(UUID tenantId, Pageable pageable);
    Page<Lead> findAllByTenantIdAndStatus(UUID tenantId, LeadStatus status, Pageable pageable);
    Page<Lead> findAllByTenantIdAndSource(UUID tenantId, String source, Pageable pageable);
    Page<Lead> findAllByTenantIdAndAssignedToUserId(UUID tenantId, UUID assignedToUserId, Pageable pageable);
    List<Lead> findAllByTenantId(UUID tenantId);
    long countByTenantIdAndStatus(UUID tenantId, LeadStatus status);

    /**
     * Case-insensitive search across name / phone / email / message. The pattern
     * argument must already be lowercased and wrapped in % wildcards.
     */
    @Query("""
            SELECT l FROM Lead l
             WHERE l.tenantId = :tenantId
               AND ( LOWER(COALESCE(l.name, ''))    LIKE :q
                  OR LOWER(COALESCE(l.phone, ''))   LIKE :q
                  OR LOWER(COALESCE(l.email, ''))   LIKE :q
                  OR LOWER(COALESCE(l.message, '')) LIKE :q )
            """)
    Page<Lead> search(@Param("tenantId") UUID tenantId, @Param("q") String pattern, Pageable pageable);
}
