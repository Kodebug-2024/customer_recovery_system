package com.codezilla.crm.message;

import com.codezilla.crm.lead.Lead;
import com.codezilla.crm.lead.LeadService;
import com.codezilla.crm.messaging.MessagingService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leads/{leadId}/messages")
public class MessageController {

    private final MessageService service;
    private final LeadService leads;
    private final MessagingService messaging;

    public MessageController(MessageService service, LeadService leads, MessagingService messaging) {
        this.service = service;
        this.leads = leads;
        this.messaging = messaging;
    }

    public record MessageView(UUID id, MessageDirection direction, String channel,
                              String content, Instant createdAt) {
        static MessageView from(Message m) {
            return new MessageView(m.getId(), m.getDirection(), m.getChannel(),
                    m.getContent(), m.getCreatedAt());
        }
    }

    public record ReplyRequest(@NotBlank String content, String channel) {}

    @GetMapping
    public List<MessageView> list(@PathVariable UUID leadId) {
        return service.conversation(leadId).stream().map(MessageView::from).toList();
    }

    @PostMapping
    public MessageView reply(@PathVariable UUID leadId, @RequestBody ReplyRequest body) {
        Lead lead = leads.get(leadId);
        String channel = body.channel() == null || body.channel().isBlank() ? "whatsapp" : body.channel();
        messaging.sendText(lead, body.content(), channel);
        return MessageView.from(service.conversation(leadId).get(service.conversation(leadId).size() - 1));
    }
}
