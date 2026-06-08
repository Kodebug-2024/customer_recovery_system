package com.codezilla.crm.lead;

import com.codezilla.crm.audit.AuditService;
import com.codezilla.crm.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api")
public class TagController {

    private final TagRepository tags;
    private final LeadService leads;
    private final AuditService audit;

    public TagController(TagRepository tags, LeadService leads, AuditService audit) {
        this.tags = tags;
        this.leads = leads;
        this.audit = audit;
    }

    public record TagView(UUID id, String name, String color, Instant createdAt) {
        static TagView from(Tag t) { return new TagView(t.getId(), t.getName(), t.getColor(), t.getCreatedAt()); }
    }
    public record TagRequest(@NotBlank String name, String color) {}
    public record AttachRequest(@NotBlank String name, String color) {}

    // ----- Tenant tag catalog -----

    @GetMapping("/tags")
    public List<TagView> list() {
        return tags.findAllByTenantIdOrderByNameAsc(TenantContext.require())
                .stream().map(TagView::from).toList();
    }

    @PostMapping("/tags")
    @Transactional
    public TagView create(@Valid @RequestBody TagRequest req) {
        UUID t = TenantContext.require();
        if (tags.findByTenantIdAndName(t, req.name().trim()).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "Tag already exists");
        }
        Tag tag = new Tag();
        tag.setName(req.name().trim());
        tag.setColor(req.color());
        tags.save(tag);
        return TagView.from(tag);
    }

    @DeleteMapping("/tags/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        Tag tag = tags.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (!tag.getTenantId().equals(TenantContext.require()))
            throw new ResponseStatusException(NOT_FOUND);
        tags.delete(tag);
        return ResponseEntity.noContent().build();
    }

    // ----- Lead-scoped tag operations -----

    @GetMapping("/leads/{leadId}/tags")
    public List<TagView> tagsForLead(@PathVariable UUID leadId) {
        leads.get(leadId);
        return tags.findAllByLeadId(leadId).stream().map(TagView::from).toList();
    }

    /**
     * Attach a tag by name. Creates the tag if it doesn't exist yet.
     * This is the most common UX: typing a name in a combobox.
     */
    @PostMapping("/leads/{leadId}/tags")
    @Transactional
    public TagView attach(@PathVariable UUID leadId, @Valid @RequestBody AttachRequest req) {
        leads.get(leadId);
        UUID t = TenantContext.require();
        Tag tag = tags.findByTenantIdAndName(t, req.name().trim()).orElseGet(() -> {
            Tag n = new Tag();
            n.setName(req.name().trim());
            n.setColor(req.color());
            return tags.save(n);
        });
        tags.attach(leadId, tag.getId());
        audit.record("lead", leadId, "TAG_ADD", tag.getName());
        return TagView.from(tag);
    }

    @DeleteMapping("/leads/{leadId}/tags/{tagId}")
    @Transactional
    public ResponseEntity<Void> detach(@PathVariable UUID leadId, @PathVariable UUID tagId) {
        leads.get(leadId);
        Tag tag = tags.findById(tagId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (!tag.getTenantId().equals(TenantContext.require()))
            throw new ResponseStatusException(NOT_FOUND);
        tags.detach(leadId, tagId);
        audit.record("lead", leadId, "TAG_REMOVE", tag.getName());
        return ResponseEntity.noContent().build();
    }
}
