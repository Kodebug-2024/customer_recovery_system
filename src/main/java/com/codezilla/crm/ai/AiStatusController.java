package com.codezilla.crm.ai;

import com.codezilla.crm.ai.faq.FaqRepository;
import com.codezilla.crm.tenant.TenantContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Status of the AI gateway for the current tenant. Used by the dashboard's
 * /settings page to show which providers are live and which tier will answer.
 */
@RestController
@RequestMapping("/api/ai")
public class AiStatusController {

    private final List<LlmProvider> providers;
    private final FaqRepository faqs;

    public AiStatusController(List<LlmProvider> providers, FaqRepository faqs) {
        this.providers = providers;
        this.faqs = faqs;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "faqCount", faqs.findAllByTenantIdOrderByPriorityDescCreatedAtDesc(TenantContext.require()).size(),
                "providers", providers.stream().map(p -> Map.of(
                        "name", p.name(),
                        "available", p.isAvailable())).toList());
    }
}
