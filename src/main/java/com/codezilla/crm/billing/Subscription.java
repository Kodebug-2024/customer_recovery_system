package com.codezilla.crm.billing;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
public class Subscription {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false, unique = true)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", length = 32, nullable = false)
    private Plan plan = Plan.FREE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "stripe_customer_id", length = 80)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", length = 80)
    private String stripeSubscriptionId;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
