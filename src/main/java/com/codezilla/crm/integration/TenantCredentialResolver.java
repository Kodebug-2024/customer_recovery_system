package com.codezilla.crm.integration;

import com.codezilla.crm.security.SecretCipher;
import com.codezilla.crm.tenant.Tenant;
import com.codezilla.crm.tenant.TenantContext;
import com.codezilla.crm.tenant.TenantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resolves integration credentials for the currently-bound tenant.
 *
 * Per-tenant DB-stored values take priority; if absent, fall back to the
 * server-wide env-var defaults. This lets the system support both
 * single-tenant dev/demo and multi-tenant SaaS without code changes.
 *
 * Secrets in the DB are stored AES-GCM encrypted and decrypted here only.
 */
@Component
public class TenantCredentialResolver {

    private final TenantRepository tenants;
    private final SecretCipher cipher;

    private final String envWaPhoneId;
    private final String envWaToken;
    private final String envWaVerify;
    private final String envTgToken;
    private final String envTgChat;
    private final String envOpenAiKey;

    public TenantCredentialResolver(
            TenantRepository tenants,
            SecretCipher cipher,
            @Value("${integrations.whatsapp.phone-number-id:}") String envWaPhoneId,
            @Value("${integrations.whatsapp.access-token:}") String envWaToken,
            @Value("${integrations.whatsapp.verify-token:}") String envWaVerify,
            @Value("${integrations.telegram.bot-token:}") String envTgToken,
            @Value("${integrations.telegram.default-chat-id:}") String envTgChat,
            @Value("${integrations.openai.api-key:}") String envOpenAiKey) {
        this.tenants = tenants;
        this.cipher = cipher;
        this.envWaPhoneId = envWaPhoneId;
        this.envWaToken = envWaToken;
        this.envWaVerify = envWaVerify;
        this.envTgToken = envTgToken;
        this.envTgChat = envTgChat;
        this.envOpenAiKey = envOpenAiKey;
    }

    public record WhatsAppCreds(String phoneNumberId, String accessToken, String verifyToken) {}
    public record TelegramCreds(String botToken, String chatId) {}
    public record OpenAiCreds(String apiKey) {}

    public WhatsAppCreds whatsapp() {
        Tenant t = currentTenant();
        return new WhatsAppCreds(
                pick(t == null ? null : t.getWhatsappPhoneNumberId(), envWaPhoneId),
                pickSecret(t == null ? null : t.getWhatsappAccessTokenEnc(), envWaToken),
                pickSecret(t == null ? null : t.getWhatsappVerifyTokenEnc(), envWaVerify));
    }

    public TelegramCreds telegram() {
        Tenant t = currentTenant();
        return new TelegramCreds(
                pickSecret(t == null ? null : t.getTelegramBotTokenEnc(), envTgToken),
                pick(t == null ? null : t.getTelegramChatId(), envTgChat));
    }

    public OpenAiCreds openai() {
        Tenant t = currentTenant();
        return new OpenAiCreds(
                pickSecret(t == null ? null : t.getOpenaiApiKeyEnc(), envOpenAiKey));
    }

    private Tenant currentTenant() {
        UUID id = TenantContext.get();
        return id == null ? null : tenants.findById(id).orElse(null);
    }

    private String pick(String tenantValue, String envValue) {
        return (tenantValue != null && !tenantValue.isBlank()) ? tenantValue : envValue;
    }

    private String pickSecret(byte[] enc, String envValue) {
        if (enc != null && enc.length > 0) {
            String v = cipher.decrypt(enc);
            if (v != null && !v.isBlank()) return v;
        }
        return envValue;
    }
}
