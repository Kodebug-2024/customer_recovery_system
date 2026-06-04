package com.codezilla.crm.lead;

import jakarta.validation.constraints.NotBlank;

public record LeadRequest(
        String name,
        String phone,
        String email,
        @NotBlank String source,
        String message
) {}
