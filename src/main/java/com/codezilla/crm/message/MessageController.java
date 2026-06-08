package com.codezilla.crm.message;

import com.codezilla.crm.lead.Lead;
import com.codezilla.crm.lead.LeadService;
import com.codezilla.crm.messaging.MessagingService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leads/{leadId}/messages")
public class MessageController {

    private final MessageService service;
    private final LeadService leads;
    private final MessagingService messaging;
    private final MessageEventBus bus;

    public MessageController(MessageService service, LeadService leads,
                             MessagingService messaging, MessageEventBus bus) {
        this.service = service;
        this.leads = leads;
        this.messaging = messaging;
        this.bus = bus;
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
        leads.get(leadId);
        return service.conversation(leadId).stream().map(MessageView::from).toList();
    }

    @PostMapping
    public MessageView reply(@PathVariable UUID leadId, @RequestBody ReplyRequest body) {
        Lead lead = leads.get(leadId);
        String channel = body.channel() == null || body.channel().isBlank() ? "whatsapp" : body.channel();
        messaging.sendText(lead, body.content(), channel);
        var conv = service.conversation(leadId);
        return MessageView.from(conv.get(conv.size() - 1));
    }

    /**
     * Server-Sent Events stream of new messages for a lead. Frontend uses
     * EventSource to keep the conversation view live without polling.
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable UUID leadId) {
        leads.get(leadId);
        SseEmitter emitter = new SseEmitter(0L); // never time out from server
        Disposable subscription = bus.subscribe(leadId).subscribe(
                payload -> {
                    try {
                        if (payload instanceof Message m) {
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(MessageView.from(m)));
                        }
                    } catch (IOException ex) {
                        emitter.completeWithError(ex);
                    }
                },
                emitter::completeWithError);
        emitter.onCompletion(subscription::dispose);
        emitter.onTimeout(subscription::dispose);
        emitter.onError(e -> subscription.dispose());
        return emitter;
    }
}
