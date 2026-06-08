package com.codezilla.crm.template;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tiny "{{var}}" substitution engine. Missing vars render as empty strings so
 * a missing field never breaks the message.
 */
@Service
public class TemplateRenderer {

    private static final Pattern VAR = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

    private final MessageTemplateRepository repo;

    public TemplateRenderer(MessageTemplateRepository repo) {
        this.repo = repo;
    }

    public String render(String template, Map<String, String> vars) {
        if (template == null) return "";
        Matcher m = VAR.matcher(template);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String v = vars.getOrDefault(m.group(1), "");
            m.appendReplacement(out, Matcher.quoteReplacement(v));
        }
        m.appendTail(out);
        return out.toString();
    }

    /** Find the default template for (tenant, event, channel). */
    @Transactional(readOnly = true)
    public java.util.Optional<MessageTemplate> findDefault(UUID tenantId, String event, String channel) {
        return repo.findFirstByTenantIdAndEventAndChannelAndDefaultForEventTrue(tenantId, event, channel);
    }
}
