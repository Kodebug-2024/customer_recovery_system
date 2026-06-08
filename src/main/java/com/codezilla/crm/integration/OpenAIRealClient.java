package com.codezilla.crm.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "integrations.openai.mode", havingValue = "real")
public class OpenAIRealClient implements OpenAIClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAIRealClient.class);

    private final WebClient client;
    private final String model;
    private final TenantCredentialResolver creds;

    public OpenAIRealClient(WebClient.Builder builder,
                            @Value("${integrations.openai.base-url}") String baseUrl,
                            @Value("${integrations.openai.model}") String model,
                            TenantCredentialResolver creds) {
        this.model = model;
        this.creds = creds;
        this.client = builder.baseUrl(baseUrl).build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String complete(String systemPrompt, String userMessage) {
        var c = creds.openai();
        if (c.apiKey() == null || c.apiKey().isBlank()) {
            log.warn("OpenAI call skipped: no API key for current tenant.");
            return "";
        }
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)),
                "temperature", 0.5);
        Map<String, Object> resp = client.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + c.apiKey())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if (resp == null) return "";
        List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
        if (choices == null || choices.isEmpty()) return "";
        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
        return msg == null ? "" : String.valueOf(msg.getOrDefault("content", ""));
    }
}
