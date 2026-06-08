package com.codezilla.crm.knowledge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * OpenAI text-embedding-3-small (1536 dim). Per-tenant API key resolved via
 * TenantCredentialResolver. Returns null if no key is configured for the
 * current tenant; callers should treat that as "RAG unavailable".
 */
public interface EmbeddingClient {
    /** Returns null if embeddings are unavailable for the current tenant. */
    float[] embed(String text);

    boolean isLive();
}

/** Stub: returns null so RAG is silently disabled when no provider is configured. */
@Component
@ConditionalOnProperty(name = "integrations.openai.mode", havingValue = "stub", matchIfMissing = true)
class EmbeddingStubClient implements EmbeddingClient {
    @Override public float[] embed(String text) { return null; }
    @Override public boolean isLive() { return false; }
}

@Component
@ConditionalOnProperty(name = "integrations.openai.mode", havingValue = "real")
class OpenAiEmbeddingClient implements EmbeddingClient {

    private final WebClient client;
    private final com.codezilla.crm.integration.TenantCredentialResolver creds;
    private static final String MODEL = "text-embedding-3-small";

    OpenAiEmbeddingClient(WebClient.Builder builder,
                          @Value("${integrations.openai.base-url}") String baseUrl,
                          com.codezilla.crm.integration.TenantCredentialResolver creds) {
        this.client = builder.baseUrl(baseUrl).build();
        this.creds = creds;
    }

    @Override
    @SuppressWarnings("unchecked")
    public float[] embed(String text) {
        var c = creds.openai();
        if (c.apiKey() == null || c.apiKey().isBlank()) return null;
        if (text == null || text.isBlank()) return null;
        Map<String, Object> resp = client.post()
                .uri("/embeddings")
                .header("Authorization", "Bearer " + c.apiKey())
                .bodyValue(Map.of("model", MODEL, "input", text))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if (resp == null) return null;
        List<Map<String, Object>> data = (List<Map<String, Object>>) resp.get("data");
        if (data == null || data.isEmpty()) return null;
        List<Number> emb = (List<Number>) data.get(0).get("embedding");
        if (emb == null) return null;
        float[] out = new float[emb.size()];
        for (int i = 0; i < out.length; i++) out[i] = emb.get(i).floatValue();
        return out;
    }

    @Override public boolean isLive() { return true; }
}
