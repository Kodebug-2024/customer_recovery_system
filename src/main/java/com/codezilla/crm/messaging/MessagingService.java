package com.codezilla.crm.messaging;

import com.codezilla.crm.integration.WhatsAppClient;
import com.codezilla.crm.lead.Lead;
import com.codezilla.crm.message.MessageDirection;
import com.codezilla.crm.message.MessageService;
import com.codezilla.crm.template.MessageTemplate;
import com.codezilla.crm.template.TemplateRenderer;
import com.codezilla.crm.tenant.Tenant;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class MessagingService {

    private static final String FALLBACK =
            "Hi {{name}}, thanks for contacting us. We will get back to you shortly.";

    private final WhatsAppClient whatsapp;
    private final MessageService messages;
    private final TemplateRenderer renderer;

    public MessagingService(WhatsAppClient whatsapp, MessageService messages,
                            TemplateRenderer renderer) {
        this.whatsapp = whatsapp;
        this.messages = messages;
        this.renderer = renderer;
    }

    public void sendAutoReply(Tenant tenant, Lead lead) {
        if (lead.getPhone() == null || lead.getPhone().isBlank()) return;

        // Resolution order: default template for (whatsapp, auto_reply) → tenant.autoReplyTemplate → hard-coded fallback.
        String template = renderer.findDefault(tenant.getId(), "auto_reply", "whatsapp")
                .map(MessageTemplate::getBody)
                .filter(s -> !s.isBlank())
                .orElseGet(() -> {
                    String t = tenant.getAutoReplyTemplate();
                    return (t == null || t.isBlank()) ? FALLBACK : t;
                });

        Map<String, String> vars = new HashMap<>();
        vars.put("name", lead.getName() == null ? "there" : lead.getName());
        vars.put("phone", lead.getPhone() == null ? "" : lead.getPhone());
        vars.put("email", lead.getEmail() == null ? "" : lead.getEmail());
        vars.put("source", lead.getSource() == null ? "" : lead.getSource());
        vars.put("business", tenant.getName() == null ? "" : tenant.getName());

        String body = renderer.render(template, vars);
        whatsapp.sendText(lead.getPhone(), body);
        messages.record(lead.getId(), MessageDirection.OUTBOUND, "whatsapp", body);
    }

    public void sendText(Lead lead, String body, String channel) {
        if ("whatsapp".equalsIgnoreCase(channel) && lead.getPhone() != null) {
            whatsapp.sendText(lead.getPhone(), body);
        }
        messages.record(lead.getId(), MessageDirection.OUTBOUND, channel, body);
    }

    /**
     * Send a pre-approved WhatsApp template (required by Meta for sends
     * outside the 24h customer-service window).
     */
    public void sendWhatsAppTemplate(Lead lead, String templateName, String languageCode,
                                     java.util.List<String> parameters) {
        if (lead.getPhone() == null || lead.getPhone().isBlank()) return;
        whatsapp.sendTemplate(lead.getPhone(), templateName, languageCode, parameters);
        String preview = "[template:" + templateName + "] "
                + (parameters == null ? "" : String.join(" | ", parameters));
        messages.record(lead.getId(), MessageDirection.OUTBOUND, "whatsapp", preview);
    }
}
