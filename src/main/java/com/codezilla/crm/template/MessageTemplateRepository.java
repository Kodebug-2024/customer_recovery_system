package com.codezilla.crm.template;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, UUID> {
    List<MessageTemplate> findAllByTenantIdOrderByEventAscNameAsc(UUID tenantId);
    Optional<MessageTemplate> findFirstByTenantIdAndEventAndChannelAndDefaultForEventTrue(
            UUID tenantId, String event, String channel);
}
