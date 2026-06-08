package com.codezilla.crm.lead;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class LeadNotFoundException extends RuntimeException {
    public LeadNotFoundException(UUID id) {
        super("Lead not found: " + id);
    }
}
