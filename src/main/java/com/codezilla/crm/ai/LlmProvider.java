package com.codezilla.crm.ai;

/**
 * Generic LLM provider abstraction. Implementations: stub, Ollama (local),
 * OpenAI (cloud). Selected per-tenant at runtime by {@link LlmRouter}.
 *
 * Implementations MUST NOT throw on transport errors — return null instead so
 * the router can fall back to the next provider.
 */
public interface LlmProvider {

    /** Provider name used in tenant.ai_provider (faq | ollama | openai). */
    String name();

    /** True when the provider is configured + reachable for the current tenant. */
    boolean isAvailable();

    /** Returns the assistant reply, or null on any failure. */
    String complete(String systemPrompt, String userMessage);
}
