package com.codezilla.crm.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "integrations.telegram.mode", havingValue = "real")
public class TelegramBotClient implements TelegramClient {

    private final WebClient client;
    private final String token;

    public TelegramBotClient(WebClient.Builder builder,
                             @Value("${integrations.telegram.base-url}") String baseUrl,
                             @Value("${integrations.telegram.bot-token}") String token) {
        this.token = token;
        this.client = builder.baseUrl(baseUrl).build();
    }

    @Override
    public void sendMessage(String chatId, String text) {
        client.post()
                .uri("/bot{token}/sendMessage", token)
                .bodyValue(Map.of("chat_id", chatId, "text", text))
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
