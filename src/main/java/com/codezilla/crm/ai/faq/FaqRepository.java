package com.codezilla.crm.ai.faq;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface FaqRepository extends JpaRepository<FaqEntry, UUID> {

    /** Highest priority first, then longest pattern (more specific wins ties). */
    @Query("SELECT f FROM FaqEntry f WHERE f.tenantId = :tenantId ORDER BY f.priority DESC, LENGTH(f.pattern) DESC")
    List<FaqEntry> findAllForMatching(UUID tenantId);

    List<FaqEntry> findAllByTenantIdOrderByPriorityDescCreatedAtDesc(UUID tenantId);

    @Modifying
    @Query("UPDATE FaqEntry f SET f.hitCount = f.hitCount + 1 WHERE f.id = :id")
    void incrementHitCount(UUID id);
}
