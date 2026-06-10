package com.codezilla.crm.ai.faq;

import com.codezilla.crm.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/faqs")
public class FaqController {

    private final FaqRepository repo;

    public FaqController(FaqRepository repo) {
        this.repo = repo;
    }

    public record FaqView(UUID id, String pattern, String reply, int priority,
                          long hitCount, Instant updatedAt) {
        static FaqView from(FaqEntry f) {
            return new FaqView(f.getId(), f.getPattern(), f.getReply(), f.getPriority(),
                    f.getHitCount(), f.getUpdatedAt());
        }
    }

    public record SaveRequest(@NotBlank String pattern, @NotBlank String reply, Integer priority) {}

    @GetMapping
    public List<FaqView> list() {
        return repo.findAllByTenantIdOrderByPriorityDescCreatedAtDesc(TenantContext.require())
                .stream().map(FaqView::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public FaqView create(@Valid @RequestBody SaveRequest req) {
        FaqEntry f = new FaqEntry();
        f.setPattern(req.pattern().trim());
        f.setReply(req.reply().trim());
        f.setPriority(req.priority() == null ? 0 : req.priority());
        repo.save(f);
        return FaqView.from(f);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public FaqView update(@PathVariable UUID id, @Valid @RequestBody SaveRequest req) {
        FaqEntry f = repo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (!f.getTenantId().equals(TenantContext.require()))
            throw new ResponseStatusException(NOT_FOUND);
        f.setPattern(req.pattern().trim());
        f.setReply(req.reply().trim());
        if (req.priority() != null) f.setPriority(req.priority());
        return FaqView.from(f);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(@PathVariable UUID id) {
        FaqEntry f = repo.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (!f.getTenantId().equals(TenantContext.require()))
            throw new ResponseStatusException(NOT_FOUND);
        repo.delete(f);
    }
}
