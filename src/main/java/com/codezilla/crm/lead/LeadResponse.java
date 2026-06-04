package com.codezilla.crm.lead;

import java.time.Instant;
import java.util.UUID;

public record LeadResponse(
        UUID id,
        String name,
        String phone,
        String email,
        String source,
        String message,
        LeadStatus status,
        String assignedTo,
        Instant createdAt,
        Instant updatedAt
) {
    public static LeadResponse from(Lead l) {
        return new LeadResponse(
                l.getId(), l.getName(), l.getPhone(), l.getEmail(),
                l.getSource(), l.getMessage(), l.getStatus(),
                l.getAssignedTo(), l.getCreatedAt(), l.getUpdatedAt());
    }
}
