package com.codezilla.crm.message;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leads/{leadId}/messages")
public class MessageController {

    private final MessageService service;

    public MessageController(MessageService service) {
        this.service = service;
    }

    public record MessageView(UUID id, MessageDirection direction, String channel,
                              String content, Instant createdAt) {
        static MessageView from(Message m) {
            return new MessageView(m.getId(), m.getDirection(), m.getChannel(),
                    m.getContent(), m.getCreatedAt());
        }
    }

    @GetMapping
    public List<MessageView> list(@PathVariable UUID leadId) {
        return service.conversation(leadId).stream().map(MessageView::from).toList();
    }
}
