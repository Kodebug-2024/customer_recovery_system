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

    @Override
    public void sendTemplate(String toPhone, String templateName, String languageCode,
                             java.util.List<String> parameters) {
        var c = creds.whatsapp();
        if (c.accessToken() == null || c.accessToken().isBlank()
                || c.phoneNumberId() == null || c.phoneNumberId().isBlank()) {
            log.warn("WhatsApp template send skipped: no credentials configured for current tenant.");
            return;
        }
        String to = toPhone == null ? null : toPhone.replaceAll("[^0-9]", "");
        String lang = (languageCode == null || languageCode.isBlank()) ? "en" : languageCode;

        // Meta payload: components[].parameters[].text — one per {{n}} placeholder in the body.
        java.util.Map<String, Object> template = new java.util.HashMap<>();
        template.put("name", templateName);
        template.put("language", Map.of("code", lang));
        if (parameters != null && !parameters.isEmpty()) {
            java.util.List<Map<String, Object>> params = new java.util.ArrayList<>();
            for (String p : parameters) params.add(Map.of("type", "text", "text", p == null ? "" : p));
            template.put("components", java.util.List.of(Map.of("type", "body", "parameters", params)));
        }

        try {
            String response = client.post()
                    .uri("/{id}/messages", c.phoneNumberId())
                    .header("Authorization", "Bearer " + c.accessToken())
                    .bodyValue(Map.of(
                            "messaging_product", "whatsapp",
                            "recipient_type", "individual",
                            "to", to,
                            "type", "template",
                            "template", template))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("WhatsApp template send OK to={} template={} response={}", to, templateName, response);
        } catch (WebClientResponseException e) {
            log.error("WhatsApp template send FAILED to={} template={} status={} body={}",
                    to, templateName, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }
}
