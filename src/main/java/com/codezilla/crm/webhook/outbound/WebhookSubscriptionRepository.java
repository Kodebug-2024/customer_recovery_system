package com.codezilla.crm.webhook.outbound;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, UUID> {
    List<WebhookSubscription> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<WebhookSubscription> findAllByTenantIdAndEnabledTrue(UUID tenantId);
}
