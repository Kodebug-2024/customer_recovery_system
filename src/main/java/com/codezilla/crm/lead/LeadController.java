package com.codezilla.crm.lead;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadService service;

    public LeadController(LeadService service) {
        this.service = service;
    }

    @GetMapping
    public Page<LeadResponse> list(@RequestParam(required = false) LeadStatus status,
                                   @RequestParam(required = false) String source,
                                   Pageable pageable) {
        return service.list(status, source, pageable).map(LeadResponse::from);
    }

    @GetMapping("/{id}")
    public LeadResponse get(@PathVariable UUID id) {
        return LeadResponse.from(service.get(id));
    }

    @PostMapping
    public ResponseEntity<LeadResponse> create(@Valid @RequestBody LeadRequest req) {
        Lead lead = service.create(req);
        return ResponseEntity.ok(LeadResponse.from(lead));
    }

    public record StatusUpdate(LeadStatus status) {}

    @PatchMapping("/{id}/status")
    public LeadResponse updateStatus(@PathVariable UUID id, @RequestBody StatusUpdate body) {
        return LeadResponse.from(service.updateStatus(id, body.status()));
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
