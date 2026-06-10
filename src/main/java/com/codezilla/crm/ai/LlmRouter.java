package com.codezilla.crm.ai;

import com.codezilla.crm.ai.faq.FaqEntry;
import com.codezilla.crm.ai.faq.FaqMatcher;
import com.codezilla.crm.billing.BillingGate;
import com.codezilla.crm.tenant.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 3-tier AI reply gateway. For each incoming lead message:
 *
 * <ol>
 *   <li><b>FAQ engine</b> (always tried first if the tenant has any FAQs) —
 *       deterministic, zero-cost, zero-latency. Best for "what are your
 *       hours?", "do you ship to X?".</li>
 *   <li><b>Ollama</b> (local LLM) — free, runs in our own container.
 *       Default for Free plan customers and anyone with {@code ai_provider=auto}.</li>
 *   <li><b>OpenAI</b> — paid, higher quality. Only used when the tenant
 *       explicitly chose {@code openai} AND is on a paid plan.</li>
 * </ol>
 *
 * The tenant's {@code ai_provider} column can pin the choice:
 * <ul>
 *   <li>{@code auto} = FAQ → global default (Ollama) → OpenAI fallback if available</li>
 *   <li>{@code faq}    = FAQ only; no LLM</li>
 *   <li>{@code ollama} = FAQ → Ollama only</li>
 *   <li>{@code openai} = FAQ → OpenAI only (requires Pro plan); falls back to
 *                        Ollama if billing blocks or API key missing</li>
 * </ul>
 */
@Service
public class LlmRouter {

    private static final Logger log = LoggerFactory.getLogger(LlmRouter.class);

    private final FaqMatcher faqs;
    private final Map<String, LlmProvider> providersByName;
    private final BillingGate billing;
    private final String defaultProvider;

    public LlmRouter(FaqMatcher faqs,
                     List<LlmProvider> providers,
                     BillingGate billing,
                     @Value("${ai.default-provider:ollama}") String defaultProvider) {
        this.faqs = faqs;
        this.billing = billing;
        this.defaultProvider = defaultProvider;
        this.providersByName = new LinkedHashMap<>();
        for (LlmProvider p : providers) providersByName.put(p.name(), p);
    }

    public record Reply(String text, String source) {}

    public Optional<Reply> respond(Tenant tenant, String systemPrompt, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return Optional.empty();

        // Tier 1: FAQ. Skips the LLM entirely on a hit — free + instant.
        Optional<FaqEntry> faqHit = faqs.match(tenant.getId(), userMessage);
        if (faqHit.isPresent()) {
            log.debug("FAQ hit for tenant {}: pattern='{}'", tenant.getId(), faqHit.get().getPattern());
            return Optional.of(new Reply(faqHit.get().getReply(), "faq"));
        }

        // Tier 2/3: LLM, in fallback order determined by tenant preference.
        for (String name : resolveProviderOrder(tenant)) {
            LlmProvider p = providersByName.get(name);
            if (p == null || !p.isAvailable()) continue;
            String text = p.complete(systemPrompt, userMessage);
            if (text != null && !text.isBlank()) {
                log.debug("LLM reply via {} for tenant {}", p.name(), tenant.getId());
                return Optional.of(new Reply(text, p.name()));
            }
        }
        return Optional.empty();
    }

    /** Returns provider names in fallback order. Empty for "faq" — FAQ only. */
    private List<String> resolveProviderOrder(Tenant tenant) {
        String pref = tenant.getAiProvider() == null ? "auto" : tenant.getAiProvider();
        return switch (pref) {
            case "faq"    -> List.of();
            case "ollama" -> List.of("ollama");
            case "openai" -> {
                // OpenAI requires a paid plan. Free plan silently falls back to Ollama.
                if (billing.aiAllowed()) yield List.of("openai", "ollama");
                else yield List.of("ollama");
            }
            default -> {
                // auto: try the configured default first, then the other LLM as fallback.
                if ("openai".equals(defaultProvider) && billing.aiAllowed())
                    yield List.of("openai", "ollama");
                yield List.of("ollama", billing.aiAllowed() ? "openai" : null)
                        .stream().filter(java.util.Objects::nonNull).toList();
            }
        };
    }
}
