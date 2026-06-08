package com.codezilla.crm.api;

import com.codezilla.crm.lead.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Public REST API. Authentication is via Bearer API key (handled by
 * {@link com.codezilla.crm.apikey.ApiKeyAuthFilter}). All responses honor
 * the role of the user who owns the key — so a VIEWER's key can list leads
 * but not delete them.
 *
 * Versioned at /v1/* so we can ship breaking changes later under /v2/*.
 */
@RestController
@RequestMapping("/v1")
public class PublicApiController {

    private final LeadService leads;

    public PublicApiController(LeadService leads) {
        this.leads = leads;
    }

    public record CreateLeadRequest(
            @NotBlank String name,
            String phone,
            String email,
            String source,
            String message) {}

    @GetMapping("/leads")
    public Page<LeadResponse> listLeads(@RequestParam(required = false) LeadStatus status,
                                        @RequestParam(required = false) String source,
                                        @RequestParam(required = false) String q,
                                        Pageable pageable) {
        if (q != null && !q.isBlank()) return leads.search(q, pageable).map(LeadResponse::from);
        return leads.list(status, source, pageable).map(LeadResponse::from);
    }

    @GetMapping("/leads/{id}")
    public LeadResponse getLead(@PathVariable UUID id) {
        return LeadResponse.from(leads.get(id));
    }

    @PostMapping("/leads")
    public ResponseEntity<LeadResponse> createLead(@Valid @RequestBody CreateLeadRequest req) {
        Lead lead = leads.create(new LeadRequest(
                req.name(), req.phone(), req.email(),
                req.source() == null || req.source().isBlank() ? "api" : req.source(),
                req.message()));
        return ResponseEntity.ok(LeadResponse.from(lead));
    }

    public record StatusUpdate(@NotBlank String status) {}

    @PatchMapping("/leads/{id}/status")
    public LeadResponse updateLeadStatus(@PathVariable UUID id, @RequestBody StatusUpdate body) {
        return LeadResponse.from(leads.updateStatus(id, LeadStatus.valueOf(body.status().toUpperCase())));
    }
}
