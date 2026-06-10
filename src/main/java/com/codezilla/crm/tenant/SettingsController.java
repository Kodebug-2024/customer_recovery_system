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
            String name, String industry, boolean aiEnabled, String aiProvider,
            String autoReplyTemplate, boolean webhookSecretConfigured,
            String whatsappPhoneNumberId,
            boolean whatsappAccessTokenConfigured,
            boolean whatsappVerifyTokenConfigured,
            boolean telegramBotTokenConfigured,
            String telegramChatId,
            boolean openaiApiKeyConfigured,
            String bookingSlug,
            boolean bookingEnabled,
            String bookingBlurb,
            java.time.Instant onboardingCompletedAt) {}

    /**
     * For sensitive fields: null = leave unchanged, "" (empty string) = clear,
     * anything else = set to that value (will be encrypted).
     */
    public record SettingsUpdate(
            String name, String industry, Boolean aiEnabled, String aiProvider,
            String autoReplyTemplate, String webhookSecret,
            String whatsappPhoneNumberId,
            String whatsappAccessToken,
            String whatsappVerifyToken,
            String telegramBotToken,
            String telegramChatId,
            String openaiApiKey,
            String bookingSlug,
            Boolean bookingEnabled,
            String bookingBlurb) {}

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
        if (body.aiProvider() != null) {
            String p = body.aiProvider().trim().toLowerCase();
            if (!java.util.Set.of("auto", "faq", "ollama", "openai").contains(p))
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "ai_provider must be one of: auto, faq, ollama, openai");
            t.setAiProvider(p);
        }
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

        if (body.bookingSlug() != null) {
            String slug = body.bookingSlug().trim().toLowerCase().replaceAll("[^a-z0-9-]", "-");
            t.setBookingSlug(slug.isEmpty() ? null : slug);
        }
        if (body.bookingEnabled() != null) t.setBookingEnabled(body.bookingEnabled());
        if (body.bookingBlurb() != null)
            t.setBookingBlurb(body.bookingBlurb().isBlank() ? null : body.bookingBlurb());

        tenants.save(t);
        return view(t);
    }

    private byte[] encOrClear(String value) {
        return (value == null || value.isBlank()) ? null : cipher.encrypt(value);
    }

    private SettingsView view(Tenant t) {
        return new SettingsView(
                t.getName(), t.getIndustry(), t.isAiEnabled(), t.getAiProvider(),
                t.getAutoReplyTemplate(),
                t.getWebhookSecret() != null && !t.getWebhookSecret().isBlank(),
                t.getWhatsappPhoneNumberId(),
                t.getWhatsappAccessTokenEnc() != null && t.getWhatsappAccessTokenEnc().length > 0,
                t.getWhatsappVerifyTokenEnc() != null && t.getWhatsappVerifyTokenEnc().length > 0,
                t.getTelegramBotTokenEnc() != null && t.getTelegramBotTokenEnc().length > 0,
                t.getTelegramChatId(),
                t.getOpenaiApiKeyEnc() != null && t.getOpenaiApiKeyEnc().length > 0,
                t.getBookingSlug(),
                t.isBookingEnabled(),
                t.getBookingBlurb(),
                t.getOnboardingCompletedAt());
    }

    @PostMapping("/complete-onboarding")
    public SettingsView completeOnboarding() {
        Tenant t = tenants.findById(TenantContext.require()).orElseThrow();
        t.setOnboardingCompletedAt(java.time.Instant.now());
        tenants.save(t);
        return view(t);
    }
}
