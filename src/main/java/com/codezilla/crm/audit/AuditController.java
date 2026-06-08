package com.codezilla.crm.audit;

import com.codezilla.crm.tenant.TenantContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
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
                                 @RequestParam(required = false) String action,
                                 @RequestParam(required = false) String actor,
                                 @RequestParam(required = false) Instant from,
                                 @RequestParam(required = false) Instant to,
                                 Pageable pageable) {
        Pageable sorted = pageable.getSort().isUnsorted()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"))
                : pageable;
        return repo.findAll(
                AuditEventRepository.filtered(TenantContext.require(), entityType, entityId, action, actor, from, to),
                sorted);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public void export(@RequestParam(required = false) String entityType,
                       @RequestParam(required = false) UUID entityId,
                       @RequestParam(required = false) String action,
                       @RequestParam(required = false) String actor,
                       @RequestParam(required = false) Instant from,
                       @RequestParam(required = false) Instant to,
                       HttpServletResponse res) throws IOException {
        res.setContentType("text/csv; charset=utf-8");
        res.setHeader("Content-Disposition", "attachment; filename=\"audit.csv\"");
        try (PrintWriter w = res.getWriter()) {
            w.println("created_at,actor,entity_type,entity_id,action,details");
            var events = repo.findAll(
                    AuditEventRepository.filtered(TenantContext.require(), entityType, entityId, action, actor, from, to),
                    Sort.by(Sort.Direction.DESC, "createdAt"));
            for (AuditEvent e : events) {
                w.println(String.join(",",
                        csv(e.getCreatedAt() == null ? "" : e.getCreatedAt().toString()),
                        csv(e.getActor()),
                        csv(e.getEntityType()),
                        csv(e.getEntityId() == null ? "" : e.getEntityId().toString()),
                        csv(e.getAction()),
                        csv(e.getDetails())));
            }
        }
    }

    private static String csv(String s) {
        if (s == null) return "";
        boolean q = s.contains(",") || s.contains("\"") || s.contains("\n");
        String esc = s.replace("\"", "\"\"");
        return q ? "\"" + esc + "\"" : esc;
    }
}
