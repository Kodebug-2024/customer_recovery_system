package com.codezilla.crm.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "integrations.whatsapp.mode", havingValue = "real")
public class WhatsAppCloudClient implements WhatsAppClient {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppCloudClient.class);

    private final WebClient client;
    private final TenantCredentialResolver creds;

    public WhatsAppCloudClient(WebClient.Builder builder,
                               @Value("${integrations.whatsapp.base-url}") String baseUrl,
                               TenantCredentialResolver creds) {
        this.creds = creds;
        this.client = builder.baseUrl(baseUrl).build();
    }

    @Override
    public void sendText(String toPhone, String body) {
        var c = creds.whatsapp();
        if (c.accessToken() == null || c.accessToken().isBlank()
                || c.phoneNumberId() == null || c.phoneNumberId().isBlank()) {
            log.warn("WhatsApp send skipped: no credentials configured for current tenant.");
            return;
        }
        String to = toPhone == null ? null : toPhone.replaceAll("[^0-9]", "");
        try {
            String response = client.post()
                    .uri("/{id}/messages", c.phoneNumberId())
                    .header("Authorization", "Bearer " + c.accessToken())
                    .bodyValue(Map.of(
                            "messaging_product", "whatsapp",
                            "recipient_type", "individual",
                            "to", to,
                            "type", "text",
                            "text", Map.of("preview_url", false, "body", body)))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("WhatsApp send OK to={} response={}", to, response);
        } catch (WebClientResponseException e) {
            log.error("WhatsApp send FAILED to={} status={} body={}",
                    to, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }
}
