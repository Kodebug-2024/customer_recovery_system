package com.codezilla.crm.lead;

import java.util.UUID;

public class LeadNotFoundException extends RuntimeException {
    public LeadNotFoundException(UUID id) {
        super("Lead not found: " + id);
    }
}
