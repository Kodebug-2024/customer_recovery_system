package com.codezilla.crm.billing;

import com.codezilla.crm.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Dev-mode billing: bypasses Stripe and immediately upgrades the tenant to the
 * requested plan when "checkout" is called. Lets the UI flow work end-to-end
 * locally without Stripe credentials.
 */
@Service
@ConditionalOnProperty(name = "billing.mode", havingValue = "stub", matchIfMissing = true)
public class StubBillingService implements BillingService {

    private static final Logger log = LoggerFactory.getLogger(StubBillingService.class);

    private final SubscriptionRepository subs;

    public StubBillingService(SubscriptionRepository subs) {
        this.subs = subs;
    }

    @Override
    @Transactional
    public CheckoutSessionResult createCheckoutSession(Plan plan, String successUrl, String cancelUrl) {
        UUID tenantId = TenantContext.require();
        Subscription s = subs.findByTenantId(tenantId).orElseGet(() -> {
            Subscription n = new Subscription();
            n.setTenantId(tenantId);
            return n;
        });
        s.setPlan(plan);
        s.setStatus(SubscriptionStatus.ACTIVE);
        s.setStripeCustomerId("stub_cus_" + tenantId);
        s.setStripeSubscriptionId("stub_sub_" + tenantId);
        s.setCurrentPeriodEnd(Instant.now().plusSeconds(30L * 86_400L));
        s.setCancelAtPeriodEnd(false);
        subs.save(s);
        log.info("[STUB BILLING] upgraded tenant {} to {}", tenantId, plan);
        return new CheckoutSessionResult(successUrl);
    }

    @Override
    public PortalSessionResult createPortalSession(String returnUrl) {
        log.info("[STUB BILLING] portal session (returns to {})", returnUrl);
        return new PortalSessionResult(returnUrl);
    }

    @Override
    public boolean isLive() { return false; }
}
