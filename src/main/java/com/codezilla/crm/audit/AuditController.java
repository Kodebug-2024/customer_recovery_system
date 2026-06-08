package com.codezilla.crm.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditEventRepository repo;

    public AuditController(AuditEventRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public Page<AuditEvent> list(@RequestParam(required = false) String entityType,
                                 @RequestParam(required = false) UUID entityId,
                                 Pageable pageable) {
        UUID t = com.codezilla.crm.tenant.TenantContext.require();
        if (entityType != null && entityId != null) {
            return repo.findAllByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(t, entityType, entityId, pageable);
        }
        return repo.findAllByTenantIdOrderByCreatedAtDesc(t, pageable);
    }
}
