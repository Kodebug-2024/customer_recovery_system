package com.codezilla.crm.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "integrations.telegram.mode", havingValue = "real")
public class TelegramBotClient implements TelegramClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotClient.class);

    private final WebClient client;
    private final TenantCredentialResolver creds;

    public TelegramBotClient(WebClient.Builder builder,
                             @Value("${integrations.telegram.base-url}") String baseUrl,
                             TenantCredentialResolver creds) {
        this.creds = creds;
        this.client = builder.baseUrl(baseUrl).build();
    }

    @Override
    public void sendMessage(String chatId, String text) {
        var c = creds.telegram();
        if (c.botToken() == null || c.botToken().isBlank()) {
            log.warn("Telegram send skipped: no bot token for current tenant.");
            return;
        }
        client.post()
                .uri("/bot{token}/sendMessage", c.botToken())
                .bodyValue(Map.of("chat_id", chatId, "text", text))
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
