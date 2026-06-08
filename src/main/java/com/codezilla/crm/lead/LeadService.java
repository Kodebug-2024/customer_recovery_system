package com.codezilla.crm.lead;

import com.codezilla.crm.audit.AuditService;
import com.codezilla.crm.billing.BillingGate;
import com.codezilla.crm.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class LeadService {

    private static final Logger log = LoggerFactory.getLogger(LeadService.class);

    private final LeadRepository leads;
    private final ApplicationEventPublisher events;
    private final AuditService audit;
    private final BillingGate billing;

    public LeadService(LeadRepository leads, ApplicationEventPublisher events,
                       AuditService audit, BillingGate billing) {
        this.leads = leads;
        this.events = events;
        this.audit = audit;
        this.billing = billing;
    }

    /**
     * Create a lead with plan-quota enforcement.
     *
     * For UI calls (POST /api/leads) the caller is human, so blocking with
     * 402 PAYMENT REQUIRED is the right UX. For webhook ingestion we still
     * accept the lead (we can't drop a customer's WhatsApp message just
     * because the SME is over quota) but flag it in the audit log so the
     * owner sees they need to upgrade.
     */
    @Transactional
    public Lead create(LeadRequest req) {
        String block = billing.leadQuotaBlockReason();
        boolean overQuota = block != null;
        if (overQuota && req.source() != null
                && (req.source().equalsIgnoreCase("manual") || req.source().equalsIgnoreCase("api"))) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, block);
        }
        Lead lead = new Lead();
        lead.setName(req.name());
        lead.setPhone(req.phone());
        lead.setEmail(req.email());
        lead.setSource(req.source());
        lead.setMessage(req.message());
        lead.setStatus(LeadStatus.NEW);
        leads.save(lead);
        if (overQuota) {
            audit.record("lead", lead.getId(), "OVER_QUOTA", block);
            log.warn("Tenant {} over quota; accepted webhook lead {}: {}",
                    lead.getTenantId(), lead.getId(), block);
        } else {
            audit.record("lead", lead.getId(), "CREATE", "source=" + lead.getSource());
        }
        events.publishEvent(new LeadCreatedEvent(lead.getId(), lead.getTenantId()));
        return lead;
    }

    @Transactional(readOnly = true)
    public Lead get(UUID id) {
        Lead lead = leads.findById(id).orElseThrow(() -> new LeadNotFoundException(id));
        // Hibernate's @Filter does NOT apply to find-by-PK. Enforce tenant isolation here.
        if (!lead.getTenantId().equals(TenantContext.require())) {
            throw new LeadNotFoundException(id);
        }
        return lead;
    }

    @Transactional(readOnly = true)
    public Page<Lead> list(LeadStatus status, String source, Pageable pageable) {
        UUID t = TenantContext.require();
        if (status != null) return leads.findAllByTenantIdAndStatus(t, status, pageable);
        if (source != null) return leads.findAllByTenantIdAndSource(t, source, pageable);
        return leads.findAllByTenantId(t, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Lead> search(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return leads.findAllByTenantId(TenantContext.require(), pageable);
        }
        String pattern = "%" + query.toLowerCase().trim() + "%";
        return leads.search(TenantContext.require(), pattern, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Lead> listAssignedTo(UUID userId, Pageable pageable) {
        return leads.findAllByTenantIdAndAssignedToUserId(TenantContext.require(), userId, pageable);
    }

    @Transactional
    public Lead assign(UUID leadId, UUID userId) {
        Lead lead = get(leadId);
        UUID old = lead.getAssignedToUserId();
        lead.setAssignedToUserId(userId);
        audit.record("lead", leadId, "ASSIGN",
                (old == null ? "unassigned" : old.toString()) + " -> "
                        + (userId == null ? "unassigned" : userId.toString()));
        return lead;
    }

    @Transactional
    public Lead updateStatus(UUID id, LeadStatus status) {
        Lead lead = get(id);
        LeadStatus old = lead.getStatus();
        lead.setStatus(status);
        audit.record("lead", id, "STATUS_CHANGE", old + " -> " + status);
        return lead;
    }

    @Transactional
    public void softDelete(UUID id) {
        Lead lead = get(id);
        lead.setDeletedAt(Instant.now());
        audit.record("lead", id, "DELETE", null);
    }

    public UUID currentTenant() {
        return TenantContext.require();
    }
}
