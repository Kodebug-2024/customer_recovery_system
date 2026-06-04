package com.codezilla.crm.webhook;

import com.codezilla.crm.lead.Lead;
import com.codezilla.crm.lead.LeadRequest;
import com.codezilla.crm.lead.LeadService;
import com.codezilla.crm.message.MessageDirection;
import com.codezilla.crm.message.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final LeadService leads;
    private final MessageService messages;

    public WebhookController(LeadService leads, MessageService messages) {
        this.leads = leads;
        this.messages = messages;
    }

    @PostMapping("/webform")
    public ResponseEntity<Map<String, Object>> webform(@Valid @RequestBody LeadRequest req) {
        Lead lead = leads.create(req);
        if (req.message() != null && !req.message().isBlank()) {
            messages.record(lead.getId(), MessageDirection.INBOUND, "web", req.message());
        }
        return ResponseEntity.ok(Map.of("leadId", lead.getId()));
    }

    @PostMapping("/whatsapp")
    public ResponseEntity<Map<String, Object>> whatsapp(@RequestBody WhatsAppPayload payload) {
        LeadRequest req = new LeadRequest(payload.name(), payload.phone(), null, "whatsapp", payload.message());
        Lead lead = leads.create(req);
        if (payload.message() != null) {
            messages.record(lead.getId(), MessageDirection.INBOUND, "whatsapp", payload.message());
        }
        return ResponseEntity.ok(Map.of("leadId", lead.getId()));
    }

    @PostMapping("/telegram")
    public ResponseEntity<Map<String, Object>> telegram(@RequestBody TelegramPayload payload) {
        LeadRequest req = new LeadRequest(payload.name(), null, null, "telegram", payload.message());
        Lead lead = leads.create(req);
        if (payload.message() != null) {
            messages.record(lead.getId(), MessageDirection.INBOUND, "telegram", payload.message());
        }
        return ResponseEntity.ok(Map.of("leadId", lead.getId()));
    }

    public record WhatsAppPayload(String name, String phone, String message) {}
    public record TelegramPayload(String name, String chatId, String message) {}
}
