package com.codezilla.crm.lead;

import java.util.UUID;

public record LeadCreatedEvent(UUID leadId, UUID tenantId) {}
