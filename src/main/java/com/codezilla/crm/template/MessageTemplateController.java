package com.codezilla.crm.template;

import com.codezilla.crm.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/templates")
@PreAuthorize("hasRole('ADMIN')")
public class MessageTemplateController {

    private final MessageTemplateRepository repo;

    public MessageTemplateController(MessageTemplateRepository repo) {
        this.repo = repo;
    }

    public record TemplateView(UUID id, String name, String channel, String event,
                               String subject, String body, boolean isDefault,
                               Instant updatedAt) {
        static TemplateView from(MessageTemplate t) {
            return new TemplateView(t.getId(), t.getName(), t.getChannel(), t.getEvent(),
                    t.getSubject(), t.getBody(), t.isDefaultForEvent(), t.getUpdatedAt());
        }
    }
    public record TemplateRequest(@NotBlank String name, @NotBlank String channel,
                                  @NotBlank String event, String subject,
                                  @NotBlank String body, Boolean isDefault) {}

    @GetMapping
    public List<TemplateView> list() {
        return repo.findAllByTenantIdOrderByEventAscNameAsc(TenantContext.require())
                .stream().map(TemplateView::from).toList();
    }

    @PostMapping
    @Transactional
    public TemplateView create(@Valid @RequestBody TemplateRequest req) {
        MessageTemplate t = new MessageTemplate();
        apply(t, req);
        if (Boolean.TRUE.equals(req.isDefault())) clearOtherDefaults(t);
        repo.save(t);
        return TemplateView.from(t);
    }

    @PutMapping("/{id}")
    @Transactional
    public TemplateView update(@PathVariable UUID id, @Valid @RequestBody TemplateRequest req) {
        MessageTemplate t = mine(id);
        apply(t, req);
        if (Boolean.TRUE.equals(req.isDefault())) clearOtherDefaults(t);
        return TemplateView.from(t);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        repo.delete(mine(id));
        return ResponseEntity.noContent().build();
    }

    private MessageTemplate mine(UUID id) {
        MessageTemplate t = repo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (!t.getTenantId().equals(TenantContext.require()))
            throw new ResponseStatusException(NOT_FOUND);
        return t;
    }

    private void apply(MessageTemplate t, TemplateRequest req) {
        t.setName(req.name().trim());
        t.setChannel(req.channel().toLowerCase());
        t.setEvent(req.event().toLowerCase());
        t.setSubject(req.subject());
        t.setBody(req.body());
        if (req.isDefault() != null) t.setDefaultForEvent(req.isDefault());
    }

    /** Only one default per (channel, event) per tenant. */
    private void clearOtherDefaults(MessageTemplate target) {
        UUID t = TenantContext.require();
        repo.findAllByTenantIdOrderByEventAscNameAsc(t).stream()
                .filter(o -> o.isDefaultForEvent()
                        && o.getChannel().equals(target.getChannel())
                        && o.getEvent().equals(target.getEvent())
                        && !o.getId().equals(target.getId()))
                .forEach(o -> o.setDefaultForEvent(false));
    }
}
