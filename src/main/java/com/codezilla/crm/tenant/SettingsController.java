package com.codezilla.crm.tenant;

import com.codezilla.crm.security.JwtService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@PreAuthorize("hasRole('ADMIN')")
public class SettingsController {

    private final TenantRepository tenants;

    public SettingsController(TenantRepository tenants) {
        this.tenants = tenants;
    }

    public record SettingsView(String name, String industry, boolean aiEnabled,
                               String autoReplyTemplate, boolean webhookSecretConfigured) {}

    public record SettingsUpdate(String name, String industry, Boolean aiEnabled,
                                 String autoReplyTemplate, String webhookSecret) {}

    @GetMapping
    public SettingsView get() {
        Tenant t = tenants.findById(TenantContext.require()).orElseThrow();
        return new SettingsView(t.getName(), t.getIndustry(), t.isAiEnabled(),
                t.getAutoReplyTemplate(),
                t.getWebhookSecret() != null && !t.getWebhookSecret().isBlank());
    }

    @PutMapping
    public SettingsView update(@RequestBody SettingsUpdate body) {
        Tenant t = tenants.findById(TenantContext.require()).orElseThrow();
        if (body.name() != null) t.setName(body.name());
        if (body.industry() != null) t.setIndustry(body.industry());
        if (body.aiEnabled() != null) t.setAiEnabled(body.aiEnabled());
        if (body.autoReplyTemplate() != null) t.setAutoReplyTemplate(body.autoReplyTemplate());
        if (body.webhookSecret() != null) t.setWebhookSecret(body.webhookSecret().isBlank() ? null : body.webhookSecret());
        tenants.save(t);
        return get();
    }
}
