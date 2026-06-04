package com.codezilla.crm.lead;

import com.codezilla.crm.ai.AiReplyService;
import com.codezilla.crm.message.MessageDirection;
import com.codezilla.crm.message.MessageService;
import com.codezilla.crm.messaging.MessagingService;
import com.codezilla.crm.notification.NotificationService;
import com.codezilla.crm.tenant.Tenant;
import com.codezilla.crm.tenant.TenantContext;
import com.codezilla.crm.tenant.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
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

    public LeadCreatedListener(LeadRepository leads, TenantRepository tenants,
                               MessagingService messaging, AiReplyService ai,
                               NotificationService notifications, MessageService messages) {
        this.leads = leads;
        this.tenants = tenants;
        this.messaging = messaging;
        this.ai = ai;
        this.notifications = notifications;
        this.messages = messages;
    }

    @Async
    @EventListener
    public void on(LeadCreatedEvent event) {
        TenantContext.set(event.tenantId());
        try {
            Tenant tenant = tenants.findById(event.tenantId()).orElse(null);
            if (tenant == null) {
                log.warn("Tenant {} not found for lead {}", event.tenantId(), event.leadId());
                return;
            }
            Lead lead = leads.findById(event.leadId()).orElse(null);
            if (lead == null) return;

            if (tenant.isAiEnabled() && lead.getMessage() != null && !lead.getMessage().isBlank()) {
                String reply = ai.generate(tenant, lead.getMessage());
                if (reply != null && !reply.isBlank()) {
                    messaging.sendText(lead, reply, "whatsapp");
                }
            } else {
                messaging.sendAutoReply(tenant, lead);
            }

            notifications.notifyNewLead(tenant, lead);
        } catch (Exception ex) {
            log.error("Failed to process LeadCreatedEvent {}", event, ex);
        } finally {
            TenantContext.clear();
        }
    }
}
