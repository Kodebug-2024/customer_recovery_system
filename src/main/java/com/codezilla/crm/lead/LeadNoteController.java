package com.codezilla.crm.lead;

import com.codezilla.crm.audit.AuditService;
import com.codezilla.crm.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/leads/{leadId}/notes")
public class LeadNoteController {

    private final LeadNoteRepository notes;
    private final LeadService leads;
    private final AuditService audit;

    public LeadNoteController(LeadNoteRepository notes, LeadService leads, AuditService audit) {
        this.notes = notes;
        this.leads = leads;
        this.audit = audit;
    }

    public record NoteView(UUID id, UUID leadId, UUID authorUserId, String body,
                           Instant createdAt, Instant updatedAt) {
        static NoteView from(LeadNote n) {
            return new NoteView(n.getId(), n.getLeadId(), n.getAuthorUserId(),
                    n.getBody(), n.getCreatedAt(), n.getUpdatedAt());
        }
    }
    public record NoteRequest(@NotBlank String body) {}

    @GetMapping
    public List<NoteView> list(@PathVariable UUID leadId) {
        leads.get(leadId); // tenant check
        return notes.findAllByTenantIdAndLeadIdOrderByCreatedAtDesc(TenantContext.require(), leadId)
                .stream().map(NoteView::from).toList();
    }

    @PostMapping
    @Transactional
    public NoteView create(@PathVariable UUID leadId,
                           @AuthenticationPrincipal UUID authorId,
                           @Valid @RequestBody NoteRequest req) {
        leads.get(leadId);
        LeadNote n = new LeadNote();
        n.setLeadId(leadId);
        n.setAuthorUserId(authorId);
        n.setBody(req.body());
        notes.save(n);
        audit.record("lead", leadId, "NOTE_ADD", null);
        return NoteView.from(n);
    }

    @DeleteMapping("/{noteId}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID leadId, @PathVariable UUID noteId) {
        LeadNote n = notes.findById(noteId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (!n.getTenantId().equals(TenantContext.require()) || !n.getLeadId().equals(leadId)) {
            throw new ResponseStatusException(NOT_FOUND);
        }
        notes.delete(n);
        audit.record("lead", leadId, "NOTE_DELETE", null);
        return ResponseEntity.noContent().build();
    }
}
