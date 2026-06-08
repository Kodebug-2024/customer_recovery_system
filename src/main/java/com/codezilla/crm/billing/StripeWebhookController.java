package com.codezilla.crm.billing;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Receives subscription lifecycle events from Stripe. Verifies the
 * X-Stripe-Signature header against the configured webhook secret.
 *
 * Only loaded when billing is in real mode AND a webhook secret is set,
 * so it's never reachable from public Internet in dev/stub mode.
 */
@RestController
@RequestMapping("/webhooks/stripe")
@ConditionalOnProperty(name = "billing.mode", havingValue = "real")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final SubscriptionRepository subs;
    private final String webhookSecret;

    public StripeWebhookController(SubscriptionRepository subs,
                                   @Value("${billing.stripe.webhook-secret}") String webhookSecret) {
        this.subs = subs;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<String> handle(HttpServletRequest request) throws IOException {
        String signature = request.getHeader("Stripe-Signature");
        String payload = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook signature mismatch");
            return ResponseEntity.status(400).body("bad signature");
        }

        switch (event.getType()) {
            case "checkout.session.completed" -> onCheckoutCompleted(event);
            case "customer.subscription.updated",
                 "customer.subscription.created" -> onSubscriptionUpdated(event);
            case "customer.subscription.deleted" -> onSubscriptionDeleted(event);
            default -> log.debug("Ignoring Stripe event {}", event.getType());
        }
        return ResponseEntity.ok("ok");
    }

    private void onCheckoutCompleted(Event event) {
        Map<String, Object> data = rawData(event);
        String customerId = (String) data.get("customer");
        String subscriptionId = (String) data.get("subscription");
        Map<String, Object> metadata = mapOrEmpty(data.get("metadata"));
        String tenantIdStr = (String) metadata.get("tenantId");
        if (tenantIdStr == null) return;
        UUID tenantId = UUID.fromString(tenantIdStr);
        Subscription sub = subs.findByTenantId(tenantId).orElseGet(() -> {
            Subscription n = new Subscription();
            n.setTenantId(tenantId);
            return n;
        });
        sub.setStripeCustomerId(customerId);
        sub.setStripeSubscriptionId(subscriptionId);
        sub.setPlan(Plan.PRO);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        subs.save(sub);
        log.info("Checkout completed for tenant {} (sub {})", tenantId, subscriptionId);
    }

    private void onSubscriptionUpdated(Event event) {
        Map<String, Object> data = rawData(event);
        String subscriptionId = (String) data.get("id");
        String status = (String) data.get("status");
        Long currentPeriodEnd = asLong(data.get("current_period_end"));
        Boolean cancelAtPeriodEnd = (Boolean) data.get("cancel_at_period_end");

        Optional<Subscription> opt = subs.findByStripeSubscriptionId(subscriptionId);
        if (opt.isEmpty()) return;
        Subscription sub = opt.get();
        sub.setStatus(SubscriptionStatus.fromStripe(status));
        if (currentPeriodEnd != null) sub.setCurrentPeriodEnd(Instant.ofEpochSecond(currentPeriodEnd));
        if (cancelAtPeriodEnd != null) sub.setCancelAtPeriodEnd(cancelAtPeriodEnd);
        // If status is CANCELED, downgrade the plan.
        if (sub.getStatus() == SubscriptionStatus.CANCELED) sub.setPlan(Plan.FREE);
        subs.save(sub);
    }

    private void onSubscriptionDeleted(Event event) {
        Map<String, Object> data = rawData(event);
        String subscriptionId = (String) data.get("id");
        subs.findByStripeSubscriptionId(subscriptionId).ifPresent(sub -> {
            sub.setStatus(SubscriptionStatus.CANCELED);
            sub.setPlan(Plan.FREE);
            sub.setStripeSubscriptionId(null);
            subs.save(sub);
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> rawData(Event event) {
        // Avoid API version mismatches by reading the raw object as a map.
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .convertValue(event.getData().getObject(), Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapOrEmpty(Object o) {
        if (o instanceof Map<?, ?> m) return (Map<String, Object>) m;
        return Map.of();
    }

    private Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }
}
