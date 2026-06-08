package com.codezilla.crm.lead;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
                                   @RequestParam(required = false) UUID assignedToUserId,
                                   @RequestParam(required = false, defaultValue = "false") boolean mine,
                                   @RequestParam(required = false) String q,
                                   @AuthenticationPrincipal UUID currentUserId,
                                   Pageable pageable) {
        if (q != null && !q.isBlank()) {
            return service.search(q, pageable).map(LeadResponse::from);
        }
        if (mine && currentUserId != null) {
            return service.listAssignedTo(currentUserId, pageable).map(LeadResponse::from);
        }
        if (assignedToUserId != null) {
            return service.listAssignedTo(assignedToUserId, pageable).map(LeadResponse::from);
        }
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
    public record AssignRequest(UUID userId) {}

    @PatchMapping("/{id}/status")
    public LeadResponse updateStatus(@PathVariable UUID id, @RequestBody StatusUpdate body) {
        return LeadResponse.from(service.updateStatus(id, body.status()));
    }

    /** Assign or unassign (userId=null) a lead. */
    @PatchMapping("/{id}/assign")
    public LeadResponse assign(@PathVariable UUID id, @RequestBody AssignRequest body) {
        return LeadResponse.from(service.assign(id, body.userId()));
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    public record BulkIds(java.util.List<UUID> ids) {}
    public record BulkStatus(java.util.List<UUID> ids, LeadStatus status) {}
    public record BulkResult(int updated) {}

    /** Bulk soft-delete. Skips ids that aren't in the current tenant. */
    @PostMapping("/bulk-delete")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public BulkResult bulkDelete(@RequestBody BulkIds body) {
        int n = 0;
        for (UUID id : body.ids() == null ? java.util.List.<UUID>of() : body.ids()) {
            try { service.softDelete(id); n++; }
            catch (LeadNotFoundException ignored) {}
        }
        return new BulkResult(n);
    }

    /** Bulk status change. Skips ids that aren't in the current tenant. */
    @PostMapping("/bulk-status")
    public BulkResult bulkStatus(@RequestBody BulkStatus body) {
        if (body.status() == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "status required");
        int n = 0;
        for (UUID id : body.ids() == null ? java.util.List.<UUID>of() : body.ids()) {
            try { service.updateStatus(id, body.status()); n++; }
            catch (LeadNotFoundException ignored) {}
        }
        return new BulkResult(n);
    }
}
