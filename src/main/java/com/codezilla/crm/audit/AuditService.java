package com.codezilla.crm.audit;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditEventRepository repo;

    public AuditService(AuditEventRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void record(String entityType, UUID entityId, String action, String details) {
        AuditEvent e = new AuditEvent();
        e.setEntityType(entityType);
        e.setEntityId(entityId);
        e.setAction(action);
        e.setDetails(details);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        e.setActor(auth == null ? "system" : auth.getName());
        repo.save(e);
    }
}
