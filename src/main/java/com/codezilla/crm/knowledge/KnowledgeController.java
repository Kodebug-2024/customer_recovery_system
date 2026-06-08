package com.codezilla.crm.knowledge;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/knowledge")
@PreAuthorize("hasRole('ADMIN')")
public class KnowledgeController {

    private final KnowledgeService service;

    public KnowledgeController(KnowledgeService service) {
        this.service = service;
    }

    public record DocView(UUID id, String title, String content, boolean indexed,
                          Instant createdAt, Instant updatedAt) {
        static DocView from(KnowledgeDocument d) {
            return new DocView(d.getId(), d.getTitle(), d.getContent(),
                    d.getEmbedding() != null,
                    d.getCreatedAt(), d.getUpdatedAt());
        }
    }
    public record DocRequest(@NotBlank String title, @NotBlank String content) {}

    @GetMapping
    public List<DocView> list() {
        return service.list().stream().map(DocView::from).toList();
    }

    @PostMapping
    public DocView create(@Valid @RequestBody DocRequest req) {
        return DocView.from(service.createOrReplace(req.title(), req.content()));
    }

    @PostMapping("/{id}/reindex")
    public ResponseEntity<Void> reindex(@PathVariable UUID id) {
        if (!service.reindex(id)) throw new ResponseStatusException(NOT_FOUND);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!service.delete(id)) throw new ResponseStatusException(NOT_FOUND);
        return ResponseEntity.noContent().build();
    }
}
