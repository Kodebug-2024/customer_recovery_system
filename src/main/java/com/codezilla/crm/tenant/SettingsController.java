package com.codezilla.crm.tenant;

import com.codezilla.crm.security.SecretCipher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@PreAuthorize("hasRole('ADMIN')")
public class SettingsController {

    private final TenantRepository tenants;
    private final SecretCipher cipher;

    public SettingsController(TenantRepository tenants, SecretCipher cipher) {
        this.tenants = tenants;
        this.cipher = cipher;
    }

    /** Sensitive fields are returned as booleans only — never the cleartext. */
    public record SettingsView(
            String name, String industry, boolean aiEnabled,
            String autoReplyTemplate, boolean webhookSecretConfigured,
            String whatsappPhoneNumberId,
            boolean whatsappAccessTokenConfigured,
            boolean whatsappVerifyTokenConfigured,
            boolean telegramBotTokenConfigured,
            String telegramChatId,
            boolean openaiApiKeyConfigured) {}

    /**
     * For sensitive fields: null = leave unchanged, "" (empty string) = clear,
     * anything else = set to that value (will be encrypted).
     */
    public record SettingsUpdate(
            String name, String industry, Boolean aiEnabled,
            String autoReplyTemplate, String webhookSecret,
            String whatsappPhoneNumberId,
            String whatsappAccessToken,
            String whatsappVerifyToken,
            String telegramBotToken,
            String telegramChatId,
            String openaiApiKey) {}

    @GetMapping
    public SettingsView get() {
        Tenant t = tenants.findById(TenantContext.require()).orElseThrow();
        return view(t);
    }

    @PutMapping
    public SettingsView update(@RequestBody SettingsUpdate body) {
        Tenant t = tenants.findById(TenantContext.require()).orElseThrow();
        if (body.name() != null) t.setName(body.name());
        if (body.industry() != null) t.setIndustry(body.industry());
        if (body.aiEnabled() != null) t.setAiEnabled(body.aiEnabled());
        if (body.autoReplyTemplate() != null) t.setAutoReplyTemplate(body.autoReplyTemplate());
        if (body.webhookSecret() != null) t.setWebhookSecret(body.webhookSecret().isBlank() ? null : body.webhookSecret());

        if (body.whatsappPhoneNumberId() != null)
            t.setWhatsappPhoneNumberId(body.whatsappPhoneNumberId().isBlank() ? null : body.whatsappPhoneNumberId());
        if (body.telegramChatId() != null)
            t.setTelegramChatId(body.telegramChatId().isBlank() ? null : body.telegramChatId());

        if (body.whatsappAccessToken() != null) t.setWhatsappAccessTokenEnc(encOrClear(body.whatsappAccessToken()));
        if (body.whatsappVerifyToken() != null) t.setWhatsappVerifyTokenEnc(encOrClear(body.whatsappVerifyToken()));
        if (body.telegramBotToken() != null)    t.setTelegramBotTokenEnc(encOrClear(body.telegramBotToken()));
        if (body.openaiApiKey() != null)        t.setOpenaiApiKeyEnc(encOrClear(body.openaiApiKey()));

        tenants.save(t);
        return view(t);
    }

    private byte[] encOrClear(String value) {
        return (value == null || value.isBlank()) ? null : cipher.encrypt(value);
    }

    private SettingsView view(Tenant t) {
        return new SettingsView(
                t.getName(), t.getIndustry(), t.isAiEnabled(),
                t.getAutoReplyTemplate(),
                t.getWebhookSecret() != null && !t.getWebhookSecret().isBlank(),
                t.getWhatsappPhoneNumberId(),
                t.getWhatsappAccessTokenEnc() != null && t.getWhatsappAccessTokenEnc().length > 0,
                t.getWhatsappVerifyTokenEnc() != null && t.getWhatsappVerifyTokenEnc().length > 0,
                t.getTelegramBotTokenEnc() != null && t.getTelegramBotTokenEnc().length > 0,
                t.getTelegramChatId(),
                t.getOpenaiApiKeyEnc() != null && t.getOpenaiApiKeyEnc().length > 0);
    }
}
