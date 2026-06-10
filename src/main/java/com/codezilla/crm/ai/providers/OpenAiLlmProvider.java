package com.codezilla.crm.ai.providers;

import com.codezilla.crm.ai.LlmProvider;
import com.codezilla.crm.integration.TenantCredentialResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Chat Completions LLM provider. Per-tenant API key resolved at
 * call-time; falls back to env-var key. Returns null on any failure so the
 * router can try the next provider.
 */
@Component
public class OpenAiLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmProvider.class);

    private final WebClient client;
    private final String model;
    private final TenantCredentialResolver creds;

    public OpenAiLlmProvider(WebClient.Builder builder,
                             @Value("${integrations.openai.base-url:https://api.openai.com/v1}") String baseUrl,
                             @Value("${integrations.openai.model:gpt-4o-mini}") String model,
                             TenantCredentialResolver creds) {
        this.client = builder.baseUrl(baseUrl).build();
        this.model = model;
        this.creds = creds;
    }

    @Override public String name() { return "openai"; }

    @Override
    public boolean isAvailable() {
        var c = creds.openai();
        return c != null && c.apiKey() != null && !c.apiKey().isBlank();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String complete(String systemPrompt, String userMessage) {
        var c = creds.openai();
        if (c == null || c.apiKey() == null || c.apiKey().isBlank()) return null;
        try {
            Map<String, Object> resp = client.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + c.apiKey())
                    .bodyValue(Map.of(
                            "model", model,
                            "messages", List.of(
                                    Map.of("role", "system", "content", systemPrompt),
                                    Map.of("role", "user", "content", userMessage)),
                            "temperature", 0.5))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            if (resp == null) return null;
            List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
            if (choices == null || choices.isEmpty()) return null;
            Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
            return msg == null ? null : String.valueOf(msg.getOrDefault("content", "")).trim();
        } catch (Exception e) {
            log.warn("OpenAI completion failed: {}", e.toString());
            return null;
        }
    }
}
