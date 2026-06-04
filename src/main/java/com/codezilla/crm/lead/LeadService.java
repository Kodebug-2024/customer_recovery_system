package com.codezilla.crm.lead;

import com.codezilla.crm.tenant.TenantContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LeadService {

    private final LeadRepository leads;
    private final ApplicationEventPublisher events;

    public LeadService(LeadRepository leads, ApplicationEventPublisher events) {
        this.leads = leads;
        this.events = events;
    }

    @Transactional
    public Lead create(LeadRequest req) {
        Lead lead = new Lead();
        lead.setName(req.name());
        lead.setPhone(req.phone());
        lead.setEmail(req.email());
        lead.setSource(req.source());
        lead.setMessage(req.message());
        lead.setStatus(LeadStatus.NEW);
        leads.save(lead);
        events.publishEvent(new LeadCreatedEvent(lead.getId(), lead.getTenantId()));
        return lead;
    }

    @Transactional(readOnly = true)
    public Lead get(UUID id) {
        return leads.findById(id).orElseThrow(() -> new LeadNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<Lead> list(LeadStatus status, String source, Pageable pageable) {
        if (status != null) return leads.findAllByStatus(status, pageable);
        if (source != null) return leads.findAllBySource(source, pageable);
        return leads.findAll(pageable);
    }

    @Transactional
    public Lead updateStatus(UUID id, LeadStatus status) {
        Lead lead = get(id);
        lead.setStatus(status);
        return lead;
    }

    public UUID currentTenant() {
        return TenantContext.require();
    }
}
