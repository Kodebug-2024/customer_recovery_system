package com.codezilla.crm.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "integrations.whatsapp.mode", havingValue = "real")
public class WhatsAppCloudClient implements WhatsAppClient {

    private final WebClient client;
    private final String phoneNumberId;

    public WhatsAppCloudClient(WebClient.Builder builder,
                               @Value("${integrations.whatsapp.base-url}") String baseUrl,
                               @Value("${integrations.whatsapp.access-token}") String token,
                               @Value("${integrations.whatsapp.phone-number-id}") String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
        this.client = builder.baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }

    @Override
    public void sendText(String toPhone, String body) {
        client.post()
                .uri("/{id}/messages", phoneNumberId)
                .bodyValue(Map.of(
                        "messaging_product", "whatsapp",
                        "to", toPhone,
                        "type", "text",
                        "text", Map.of("body", body)))
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
