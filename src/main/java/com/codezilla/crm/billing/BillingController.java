package com.codezilla.crm.billing;

import com.codezilla.crm.tenant.TenantContext;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billing;
    private final SubscriptionRepository subs;
    private final String publicBaseUrl;

    public BillingController(BillingService billing,
                             SubscriptionRepository subs,
                             @Value("${app.public-base-url:http://localhost:3000}") String publicBaseUrl) {
        this.billing = billing;
        this.subs = subs;
        this.publicBaseUrl = publicBaseUrl;
    }

    public record BillingView(
            String plan, String status, boolean cancelAtPeriodEnd,
            Instant currentPeriodEnd, boolean live) {}

    public record CheckoutRequest(@NotNull Plan plan) {}
    public record UrlResponse(String url) {}

    @GetMapping
    public BillingView get() {
        Subscription s = subs.findByTenantId(TenantContext.require())
                .orElseGet(() -> {
                    Subscription n = new Subscription();
                    n.setTenantId(TenantContext.require());
                    n.setPlan(Plan.FREE);
                    n.setStatus(SubscriptionStatus.ACTIVE);
                    return subs.save(n);
                });
        return new BillingView(
                s.getPlan().name(),
                s.getStatus().name(),
                s.isCancelAtPeriodEnd(),
                s.getCurrentPeriodEnd(),
                billing.isLive());
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('ADMIN')")
    public UrlResponse checkout(@RequestBody CheckoutRequest req) {
        var r = billing.createCheckoutSession(
                req.plan(),
                publicBaseUrl + "/billing?status=success",
                publicBaseUrl + "/billing?status=cancel");
        return new UrlResponse(r.url());
    }

    @PostMapping("/portal")
    @PreAuthorize("hasRole('ADMIN')")
    public UrlResponse portal() {
        var r = billing.createPortalSession(publicBaseUrl + "/billing");
        return new UrlResponse(r.url());
    }

    /** Convenience map endpoint for the plan catalog (kept tiny on purpose). */
    @GetMapping("/plans")
    public Map<String, Object> plans() {
        return Map.of(
                "free", Map.of("name", "Free", "priceMonthly", 0, "limits", Map.of(
                        "leadsPerMonth", 200,
                        "aiReplies", false)),
                "pro", Map.of("name", "Pro", "priceMonthly", 29, "limits", Map.of(
                        "leadsPerMonth", 5000,
                        "aiReplies", true)));
    }
}
