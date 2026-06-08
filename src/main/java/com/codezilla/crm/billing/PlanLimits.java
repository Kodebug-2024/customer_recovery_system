package com.codezilla.crm.billing;

/**
 * Plan limits and feature flags. Kept as a single source of truth so the
 * frontend, billing UI, and gating service all agree. Update both this enum
 * and the BillingController's /plans response together.
 */
public final class PlanLimits {

    public static final int FREE_LEADS_PER_MONTH = 200;
    public static final int PRO_LEADS_PER_MONTH = 5000;

    private PlanLimits() {}

    public static int leadsPerMonth(Plan plan) {
        return plan == Plan.PRO ? PRO_LEADS_PER_MONTH : FREE_LEADS_PER_MONTH;
    }

    public static boolean aiRepliesAllowed(Plan plan) {
        return plan == Plan.PRO;
    }
}
