package com.codezilla.crm.lead;

import com.codezilla.crm.integration.SheetsExporter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/leads")
@PreAuthorize("hasRole('ADMIN')")
public class LeadSheetsController {

    private final SheetsExporter exporter;
    private final LeadRepository repo;

    public LeadSheetsController(SheetsExporter exporter, LeadRepository repo) {
        this.exporter = exporter;
        this.repo = repo;
    }

    @PostMapping("/sync-sheet")
    public Map<String, Object> sync() {
        var all = repo.findAllByTenantId(com.codezilla.crm.tenant.TenantContext.require());
        boolean ok = exporter.appendLeads(all);
        return Map.of("enabled", exporter.isEnabled(), "rows", all.size(), "ok", ok);
    }
}
