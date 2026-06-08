package com.codezilla.crm.lead;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {
    Page<Lead> findAllByStatus(LeadStatus status, Pageable pageable);
    Page<Lead> findAllBySource(String source, Pageable pageable);
    long countByStatus(LeadStatus status);
}
