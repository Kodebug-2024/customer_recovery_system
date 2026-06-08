package com.codezilla.crm.booking;

import com.codezilla.crm.lead.Lead;
import com.codezilla.crm.lead.LeadRequest;
import com.codezilla.crm.lead.LeadService;
import com.codezilla.crm.tenant.Tenant;
import com.codezilla.crm.tenant.TenantContext;
import com.codezilla.crm.tenant.TenantRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

/**
 * Public-facing booking page (no auth required) plus tenant-scoped management.
 * The /book endpoint creates a Lead (source="booking") and an Appointment in
 * the matching tenant's namespace.
 */
@RestController
public class BookingController {

    private final TenantRepository tenants;
    private final AppointmentRepository appointments;
    private final LeadService leads;

    public BookingController(TenantRepository tenants,
                             AppointmentRepository appointments,
                             LeadService leads) {
        this.tenants = tenants;
        this.appointments = appointments;
        this.leads = leads;
    }

    public record PublicTenantInfo(String name, String industry, String blurb) {}
    public record BookingRequest(
            @NotBlank String name,
            String phone,
            @Email String email,
            @NotNull Instant startsAt,
            String message,
            Integer durationMinutes) {}
    public record AppointmentView(UUID id, UUID leadId, Instant startsAt,
                                  int durationMinutes, String status, String notes) {
        static AppointmentView from(Appointment a) {
            return new AppointmentView(a.getId(), a.getLeadId(), a.getStartsAt(),
                    a.getDurationMinutes(), a.getStatus().name(), a.getNotes());
        }
    }

    // ----- Public (no auth) -----

    @GetMapping("/book/{slug}")
    public PublicTenantInfo publicInfo(@PathVariable String slug) {
        Tenant t = tenants.findByBookingSlug(slug).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (!t.isBookingEnabled()) throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Booking disabled");
        return new PublicTenantInfo(t.getName(), t.getIndustry(), t.getBookingBlurb());
    }

    @PostMapping("/book/{slug}")
    @Transactional
    public AppointmentView book(@PathVariable String slug, @Valid @RequestBody BookingRequest req) {
        Tenant t = tenants.findByBookingSlug(slug).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (!t.isBookingEnabled()) throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Booking disabled");
        if (req.startsAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Cannot book a time in the past");
        }
        // Bind tenant for LeadService.create / audit / events.
        TenantContext.set(t.getId());
        try {
            Lead lead = leads.create(new LeadRequest(req.name(), req.phone(), req.email(), "booking", req.message()));
            Appointment a = new Appointment();
            a.setTenantId(t.getId());
            a.setLeadId(lead.getId());
            a.setStartsAt(req.startsAt());
            a.setDurationMinutes(req.durationMinutes() == null ? 30 : Math.max(5, req.durationMinutes()));
            a.setStatus(AppointmentStatus.REQUESTED);
            appointments.save(a);
            return AppointmentView.from(a);
        } finally {
            TenantContext.clear();
        }
    }

    // ----- Authenticated tenant management -----

    @GetMapping("/api/appointments")
    public List<AppointmentView> list() {
        return appointments.findAllByTenantIdOrderByStartsAtAsc(TenantContext.require())
                .stream().map(AppointmentView::from).toList();
    }

    @GetMapping("/api/leads/{leadId}/appointments")
    public List<AppointmentView> listForLead(@PathVariable UUID leadId) {
        leads.get(leadId);
        return appointments.findAllByTenantIdAndLeadIdOrderByStartsAtAsc(TenantContext.require(), leadId)
                .stream().map(AppointmentView::from).toList();
    }

    public record StatusUpdate(@NotBlank String status) {}

    @PatchMapping("/api/appointments/{id}/status")
    @PreAuthorize("hasRole('AGENT')")
    @Transactional
    public AppointmentView updateStatus(@PathVariable UUID id, @RequestBody StatusUpdate body) {
        Appointment a = appointments.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (!a.getTenantId().equals(TenantContext.require())) throw new ResponseStatusException(NOT_FOUND);
        a.setStatus(AppointmentStatus.valueOf(body.status().toUpperCase()));
        return AppointmentView.from(a);
    }
}
