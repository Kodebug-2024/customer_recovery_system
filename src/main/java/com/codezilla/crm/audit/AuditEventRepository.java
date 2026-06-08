package com.codezilla.crm.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditEventRepository
        extends JpaRepository<AuditEvent, UUID>, JpaSpecificationExecutor<AuditEvent> {

    Page<AuditEvent> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<AuditEvent> findAllByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
            UUID tenantId, String entityType, UUID entityId, Pageable pageable);

    /**
     * Build a tenant-scoped Specification combining optional filters.
     * Passing null for any filter means "no constraint".
     */
    static Specification<AuditEvent> filtered(UUID tenantId, String entityType, UUID entityId,
                                              String action, String actor,
                                              Instant from, Instant to) {
        return (root, query, cb) -> {
            var preds = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            preds.add(cb.equal(root.get("tenantId"), tenantId));
            if (entityType != null && !entityType.isBlank())
                preds.add(cb.equal(root.get("entityType"), entityType));
            if (entityId != null)
                preds.add(cb.equal(root.get("entityId"), entityId));
            if (action != null && !action.isBlank())
                preds.add(cb.equal(root.get("action"), action));
            if (actor != null && !actor.isBlank())
                preds.add(cb.like(cb.lower(root.get("actor")), "%" + actor.toLowerCase() + "%"));
            if (from != null) preds.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null)   preds.add(cb.lessThan(root.get("createdAt"), to));
            return cb.and(preds.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
