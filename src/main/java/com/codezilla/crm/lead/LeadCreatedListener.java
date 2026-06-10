package com.codezilla.crm.lead;

import com.codezilla.crm.ai.AiReplyService;
import com.codezilla.crm.billing.BillingGate;
import com.codezilla.crm.message.MessageService;
import com.codezilla.crm.messaging.MessagingService;
import com.codezilla.crm.notification.NotificationService;
import com.codezilla.crm.tenant.Tenant;
import com.codezilla.crm.tenant.TenantContext;
import com.codezilla.crm.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LeadCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(LeadCreatedListener.class);

    private final LeadRepository leads;
    private final TenantRepository tenants;
    private final MessagingService messaging;
    private final AiReplyService ai;
    private final NotificationService notifications;
    private final MessageService messages;
    private final BillingGate billing;
    private final com.codezilla.crm.webhook.outbound.WebhookDeliveryService outboundWebhooks;

    public LeadCreatedListener(LeadRepository leads, TenantRepository tenants,
                               MessagingService messaging, AiReplyService ai,
                               NotificationService notifications, MessageService messages,
                               BillingGate billing,
                               com.codezilla.crm.webhook.outbound.WebhookDeliveryService outboundWebhooks) {
        this.leads = leads;
        this.tenants = tenants;
        this.messaging = messaging;
        this.ai = ai;
        this.notifications = notifications;
        this.messages = messages;
        this.billing = billing;
        this.outboundWebhooks = outboundWebhooks;
    }

    /**
     * Runs synchronously on the request thread. Each side-effect is wrapped in
     * {@link #safely} so a failure in one (e.g. WhatsApp 401) cannot block the
     * others (e.g. outbound webhooks). The slow bits — outbound webhook POSTs
     * and AI replies — are themselves {@code @Async} inside their services.
     */
    @EventListener
    public void on(LeadCreatedEvent event) {
        // Save + restore so we don't trample a TenantContext already set by an
        // upstream filter (e.g. WhatsAppSignatureFilter on the webhook path).
        java.util.UUID previous = TenantContext.get();
        TenantContext.set(event.tenantId());
        try {
            Tenant tenant = tenants.findById(event.tenantId()).orElse(null);
            if (tenant == null) {
                log.warn("Tenant {} not found for lead {}", event.tenantId(), event.leadId());
                return;
            }
            Lead lead = leads.findById(event.leadId()).orElse(null);
            if (lead == null) return;

            // Each side effect is isolated — a misconfigured WhatsApp token
            // must not block outbound webhooks from firing, and vice versa.
            safely("auto-reply", () -> {
                boolean useAi = tenant.isAiEnabled()
                        && lead.getMessage() != null && !lead.getMessage().isBlank()
                        && billing.aiAllowed();
                if (useAi) {
                    String reply = ai.generate(tenant, lead.getMessage());
                    if (reply != null && !reply.isBlank()) {
                        messaging.sendText(lead, reply, "whatsapp");
                    }
                } else {
                    messaging.sendAutoReply(tenant, lead);
                }
            });

            safely("notify", () -> notifications.notifyNewLead(tenant, lead));

            safely("outbound-webhook", () -> {
                java.util.Map<String, Object> payload = new java.util.HashMap<>();
                payload.put("id", lead.getId().toString());
                payload.put("name", lead.getName());
                payload.put("phone", lead.getPhone());
                payload.put("email", lead.getEmail());
                payload.put("source", lead.getSource());
                payload.put("status", lead.getStatus().name());
                payload.put("message", lead.getMessage());
                outboundWebhooks.publish(tenant.getId(), "lead.created", payload);
            });
        } finally {
            if (previous == null) TenantContext.clear();
            else TenantContext.set(previous);
        }
    }

    private void safely(String name, Runnable r) {
        try { r.run(); } catch (Exception e) {
            log.warn("lead-created side-effect '{}' failed: {}", name, e.toString());
        }
    }
}
