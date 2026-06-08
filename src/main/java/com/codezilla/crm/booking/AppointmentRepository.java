package com.codezilla.crm.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    List<Appointment> findAllByTenantIdOrderByStartsAtAsc(UUID tenantId);
    List<Appointment> findAllByTenantIdAndLeadIdOrderByStartsAtAsc(UUID tenantId, UUID leadId);
}
