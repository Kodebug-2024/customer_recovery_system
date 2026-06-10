package com.codezilla.crm.ai.providers;

import com.codezilla.crm.ai.LlmProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Ollama LLM provider. Talks to an Ollama server (default http://ollama:11434)
 * speaking its native /api/chat protocol. Free, runs locally — the default
 * tier for SME customers on the Free plan.
 *
 * Probes the server lazily on first {@link #isAvailable()} call and caches
 * the result for 60 seconds so we don't hammer the upstream when it's down.
 */
@Component
public class OllamaLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaLlmProvider.class);

    private final WebClient client;
    private final String model;
    private final boolean enabled;

    private volatile long lastProbeAt = 0L;
    private volatile boolean lastProbeOk = false;

    public OllamaLlmProvider(WebClient.Builder builder,
                             @Value("${integrations.ollama.base-url:http://ollama:11434}") String baseUrl,
                             @Value("${integrations.ollama.model:qwen2.5:3b}") String model,
                             @Value("${integrations.ollama.enabled:true}") boolean enabled) {
        this.client = builder.baseUrl(baseUrl).build();
        this.model = model;
        this.enabled = enabled;
    }

    @Override public String name() { return "ollama"; }

    @Override
    public boolean isAvailable() {
        if (!enabled) return false;
        long now = System.currentTimeMillis();
        if (now - lastProbeAt < 60_000) return lastProbeOk;
        lastProbeAt = now;
        try {
            client.get().uri("/api/tags").retrieve().toBodilessEntity()
                    .timeout(Duration.ofSeconds(2)).block();
            lastProbeOk = true;
        } catch (Exception e) {
            lastProbeOk = false;
        }
        return lastProbeOk;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String complete(String systemPrompt, String userMessage) {
        if (!enabled) return null;
        try {
            Map<String, Object> resp = client.post()
                    .uri("/api/chat")
                    .bodyValue(Map.of(
                            "model", model,
                            "stream", false,
                            "options", Map.of("temperature", 0.5),
                            "messages", List.of(
                                    Map.of("role", "system", "content", systemPrompt),
                                    Map.of("role", "user", "content", userMessage))))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();
            if (resp == null) return null;
            Map<String, Object> message = (Map<String, Object>) resp.get("message");
            if (message == null) return null;
            String content = String.valueOf(message.getOrDefault("content", "")).trim();
            return content.isEmpty() ? null : content;
        } catch (Exception e) {
            log.warn("Ollama completion failed (model={}): {}", model, e.toString());
            return null;
        }
    }
}
