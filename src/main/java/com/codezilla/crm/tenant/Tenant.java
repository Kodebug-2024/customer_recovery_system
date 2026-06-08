package com.codezilla.crm.tenant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants")
@Getter
@Setter
public class Tenant {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "industry")
    private String industry;

    @Column(name = "api_key", nullable = false, unique = true)
    private String apiKey;

    @Column(name = "ai_enabled", nullable = false)
    private boolean aiEnabled;

    @Column(name = "auto_reply_template", columnDefinition = "text")
    private String autoReplyTemplate;

    @Column(name = "webhook_secret")
    private String webhookSecret;

    @Column(name = "whatsapp_phone_number_id", length = 64)
    private String whatsappPhoneNumberId;

    @Column(name = "whatsapp_access_token_enc")
    private byte[] whatsappAccessTokenEnc;

    @Column(name = "whatsapp_verify_token_enc")
    private byte[] whatsappVerifyTokenEnc;

    @Column(name = "telegram_bot_token_enc")
    private byte[] telegramBotTokenEnc;

    @Column(name = "telegram_chat_id", length = 64)
    private String telegramChatId;

    @Column(name = "openai_api_key_enc")
    private byte[] openaiApiKeyEnc;

    @Column(name = "onboarding_completed_at")
    private Instant onboardingCompletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }
}
