package com.codezilla.crm.messaging;

import com.codezilla.crm.integration.WhatsAppClient;
import com.codezilla.crm.lead.Lead;
import com.codezilla.crm.message.MessageDirection;
import com.codezilla.crm.message.MessageService;
import com.codezilla.crm.tenant.Tenant;
import org.springframework.stereotype.Service;

@Service
public class MessagingService {

    private final WhatsAppClient whatsapp;
    private final MessageService messages;

    public MessagingService(WhatsAppClient whatsapp, MessageService messages) {
        this.whatsapp = whatsapp;
        this.messages = messages;
    }

    public void sendAutoReply(Tenant tenant, Lead lead) {
        if (lead.getPhone() == null || lead.getPhone().isBlank()) return;
        String template = tenant.getAutoReplyTemplate();
        if (template == null || template.isBlank()) {
            template = "Hi {{name}}, thanks for contacting us. We will get back to you shortly.";
        }
        String body = template.replace("{{name}}",
                lead.getName() == null ? "there" : lead.getName());
        whatsapp.sendText(lead.getPhone(), body);
        messages.record(lead.getId(), MessageDirection.OUTBOUND, "whatsapp", body);
    }

    public void sendText(Lead lead, String body, String channel) {
        if ("whatsapp".equalsIgnoreCase(channel) && lead.getPhone() != null) {
            whatsapp.sendText(lead.getPhone(), body);
        }
        messages.record(lead.getId(), MessageDirection.OUTBOUND, channel, body);
    }
}
