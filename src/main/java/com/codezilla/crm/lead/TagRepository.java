package com.codezilla.crm.lead;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    List<Tag> findAllByTenantIdOrderByNameAsc(UUID tenantId);
    Optional<Tag> findByTenantIdAndName(UUID tenantId, String name);

    /** Tags currently attached to a lead. */
    @Query(value = """
        SELECT t.id, t.tenant_id, t.name, t.color, t.created_at
          FROM tags t
          JOIN lead_tags lt ON lt.tag_id = t.id
         WHERE lt.lead_id = :leadId
         ORDER BY t.name
        """, nativeQuery = true)
    List<Tag> findAllByLeadId(@Param("leadId") UUID leadId);

    @Modifying
    @Query(value = "INSERT INTO lead_tags(lead_id, tag_id) VALUES (:leadId, :tagId) ON CONFLICT DO NOTHING", nativeQuery = true)
    void attach(@Param("leadId") UUID leadId, @Param("tagId") UUID tagId);

    @Modifying
    @Query(value = "DELETE FROM lead_tags WHERE lead_id = :leadId AND tag_id = :tagId", nativeQuery = true)
    void detach(@Param("leadId") UUID leadId, @Param("tagId") UUID tagId);

    @Query(value = "SELECT lead_id FROM lead_tags WHERE tag_id IN (:tagIds)", nativeQuery = true)
    Set<UUID> leadIdsWithAnyTag(@Param("tagIds") List<UUID> tagIds);
}
