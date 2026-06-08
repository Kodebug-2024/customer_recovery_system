package com.codezilla.crm.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {
    List<KnowledgeDocument> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<KnowledgeDocument> findAllByTenantId(UUID tenantId);
}
