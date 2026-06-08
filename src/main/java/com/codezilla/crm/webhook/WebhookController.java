package com.codezilla.crm.webhook;

import com.codezilla.crm.lead.Lead;
import com.codezilla.crm.lead.LeadRequest;
import com.codezilla.crm.lead.LeadService;
import com.codezilla.crm.message.MessageDirection;
import com.codezilla.crm.message.MessageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final LeadService leads;
    private final MessageService messages;
    private final String metaVerifyToken;

    public WebhookController(LeadService leads, MessageService messages,
                             @Value("${integrations.whatsapp.verify-token:}") String metaVerifyToken) {
        this.leads = leads;
        this.messages = messages;
        this.metaVerifyToken = metaVerifyToken;
    }

    // Meta WhatsApp webhook verification handshake (GET).
    @GetMapping("/whatsapp")
    public ResponseEntity<String> verifyWhatsapp(@RequestParam("hub.mode") String mode,
                                                 @RequestParam("hub.verify_token") String token,
                                                 @RequestParam("hub.challenge") String challenge) {
        if ("subscribe".equals(mode) && metaVerifyToken != null && metaVerifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(403).build();
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
    public ResponseEntity<Map<String, Object>> whatsapp(@RequestBody Map<String, Object> raw) {
        // Accept both Meta's official payload and a simple { name, phone, message } shape (for testing).
        if (raw.containsKey("object") || raw.containsKey("entry")) {
            return handleMeta(raw);
        }
        WhatsAppPayload payload = new WhatsAppPayload(
                (String) raw.get("name"),
                (String) raw.get("phone"),
                (String) raw.get("message"));
        return createLeadFromSimple(payload);
    }

    private ResponseEntity<Map<String, Object>> createLeadFromSimple(WhatsAppPayload payload) {
        LeadRequest req = new LeadRequest(payload.name(), payload.phone(), null, "whatsapp", payload.message());
        Lead lead = leads.create(req);
        if (payload.message() != null) {
            messages.record(lead.getId(), MessageDirection.INBOUND, "whatsapp", payload.message());
        }
        return ResponseEntity.ok(Map.of("leadId", lead.getId()));
    }

    private ResponseEntity<Map<String, Object>> handleMeta(Map<String, Object> raw) {
        com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
        MetaWhatsAppPayload payload = m.convertValue(raw, MetaWhatsAppPayload.class);
        java.util.List<java.util.UUID> created = new java.util.ArrayList<>();
        if (payload.entry() == null) return ResponseEntity.ok(Map.of("status", "ignored"));
        for (var entry : payload.entry()) {
            if (entry.changes() == null) continue;
            for (var change : entry.changes()) {
                var v = change.value();
                if (v == null || v.messages() == null) continue;
                String name = (v.contacts() != null && !v.contacts().isEmpty() && v.contacts().get(0).profile() != null)
                        ? v.contacts().get(0).profile().name() : null;
                for (var msg : v.messages()) {
                    String text = msg.text() != null ? msg.text().body() : null;
                    String phone = msg.from() == null ? null : "+" + msg.from();
                    LeadRequest req = new LeadRequest(name, phone, null, "whatsapp", text);
                    Lead lead = leads.create(req);
                    if (text != null) {
                        this.messages.record(lead.getId(), MessageDirection.INBOUND, "whatsapp", text);
                    }
                    created.add(lead.getId());
                }
            }
        }
        return ResponseEntity.ok(Map.of("status", "ok", "leadIds", created));
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
