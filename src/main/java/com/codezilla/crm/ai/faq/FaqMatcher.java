package com.codezilla.crm.ai.faq;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Rule-based FAQ matcher. Case-insensitive substring match — non-technical
 * users author patterns, not regex. Returns the first hit by (priority DESC,
 * pattern length DESC). Increments hit_count on match for analytics.
 */
@Service
public class FaqMatcher {

    private final FaqRepository repo;

    public FaqMatcher(FaqRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public Optional<FaqEntry> match(UUID tenantId, String message) {
        if (message == null || message.isBlank()) return Optional.empty();
        String lower = message.toLowerCase();
        for (FaqEntry f : repo.findAllForMatching(tenantId)) {
            String p = f.getPattern();
            if (p == null || p.isBlank()) continue;
            if (lower.contains(p.toLowerCase())) {
                repo.incrementHitCount(f.getId());
                return Optional.of(f);
            }
        }
        return Optional.empty();
    }
}
