package com.codezilla.crm.billing;

import com.codezilla.crm.tenant.Tenant;
import com.codezilla.crm.tenant.TenantContext;
import com.codezilla.crm.tenant.TenantRepository;
import com.codezilla.crm.user.User;
import com.codezilla.crm.user.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.billingportal.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.billingportal.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams.LineItem;
import com.stripe.param.checkout.SessionCreateParams.Mode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * Real Stripe Billing service. Active when billing.mode=real and a secret key
 * is configured. Creates Customers, Checkout Sessions, and Portal Sessions.
 * Webhook processing lives in StripeWebhookController.
 */
@Service
@ConditionalOnProperty(name = "billing.mode", havingValue = "real")
public class StripeBillingService implements BillingService {

    private static final Logger log = LoggerFactory.getLogger(StripeBillingService.class);

    private final SubscriptionRepository subs;
    private final TenantRepository tenants;
    private final UserRepository users;
    private final String secretKey;
    private final String proPriceId;

    public StripeBillingService(SubscriptionRepository subs,
                                TenantRepository tenants,
                                UserRepository users,
                                @Value("${billing.stripe.secret-key}") String secretKey,
                                @Value("${billing.stripe.pro-price-id}") String proPriceId) {
        this.subs = subs;
        this.tenants = tenants;
        this.users = users;
        this.secretKey = secretKey;
        this.proPriceId = proPriceId;
    }

    @PostConstruct
    void init() {
        Stripe.apiKey = secretKey;
        log.info("Stripe billing enabled");
    }

    @Override
    @Transactional
    public CheckoutSessionResult createCheckoutSession(Plan plan, String successUrl, String cancelUrl) {
        if (plan != Plan.PRO) {
            throw new ResponseStatusException(BAD_REQUEST, "Only PRO is a paid plan");
        }
        UUID tenantId = TenantContext.require();
        Subscription sub = subs.findByTenantId(tenantId).orElseGet(() -> {
            Subscription n = new Subscription();
            n.setTenantId(tenantId);
            return n;
        });

        try {
            String customerId = ensureStripeCustomer(sub, tenantId);

            var params = com.stripe.param.checkout.SessionCreateParams.builder()
                    .setMode(Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .addLineItem(LineItem.builder().setPrice(proPriceId).setQuantity(1L).build())
                    .setClientReferenceId(tenantId.toString())
                    .putMetadata("tenantId", tenantId.toString())
                    .build();
            var session = com.stripe.model.checkout.Session.create(params);
            subs.save(sub);
            return new CheckoutSessionResult(session.getUrl());
        } catch (StripeException e) {
            log.error("Stripe checkout failed", e);
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Stripe error: " + e.getMessage(), e);
        }
    }

    @Override
    public PortalSessionResult createPortalSession(String returnUrl) {
        UUID tenantId = TenantContext.require();
        Subscription sub = subs.findByTenantId(tenantId).orElseThrow(
                () -> new ResponseStatusException(BAD_REQUEST, "No active subscription"));
        if (sub.getStripeCustomerId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "No Stripe customer for this tenant");
        }
        try {
            var params = SessionCreateParams.builder()
                    .setCustomer(sub.getStripeCustomerId())
                    .setReturnUrl(returnUrl)
                    .build();
            Session session = Session.create(params);
            return new PortalSessionResult(session.getUrl());
        } catch (StripeException e) {
            log.error("Stripe portal failed", e);
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Stripe error: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isLive() { return true; }

    private String ensureStripeCustomer(Subscription sub, UUID tenantId) throws StripeException {
        if (sub.getStripeCustomerId() != null) return sub.getStripeCustomerId();
        Tenant tenant = tenants.findById(tenantId).orElseThrow();
        String email = users.findAllByTenantIdOrderByEmailAsc(tenantId).stream()
                .findFirst().map(User::getEmail).orElse(null);
        var params = CustomerCreateParams.builder()
                .setName(tenant.getName())
                .setEmail(email)
                .putMetadata("tenantId", tenantId.toString())
                .build();
        Customer c = Customer.create(params);
        sub.setStripeCustomerId(c.getId());
        return c.getId();
    }

    @SuppressWarnings("unused")
    private static Map<String, String> meta(String k, String v) { return Map.of(k, v); }
}
