package com.codezilla.crm.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByTenantId(UUID tenantId);
    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
    Optional<Subscription> findByStripeCustomerId(String stripeCustomerId);
}
