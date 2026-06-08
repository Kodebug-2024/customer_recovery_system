package com.codezilla.crm.billing;

/**
 * Abstracts Stripe so we can run the app without Stripe credentials in dev/test.
 * Both implementations operate on the active tenant's subscription row.
 */
public interface BillingService {

    /**
     * Create or reuse a Stripe Checkout Session for upgrading to the given plan.
     * Returns a URL the caller should redirect the user's browser to.
     */
    CheckoutSessionResult createCheckoutSession(Plan plan, String successUrl, String cancelUrl);

    /**
     * Create a Stripe Customer Portal session so the user can manage their subscription.
     * Throws if the tenant has no Stripe customer yet.
     */
    PortalSessionResult createPortalSession(String returnUrl);

    /** Whether this implementation is the real Stripe-backed one. */
    boolean isLive();

    record CheckoutSessionResult(String url) {}
    record PortalSessionResult(String url) {}
}
