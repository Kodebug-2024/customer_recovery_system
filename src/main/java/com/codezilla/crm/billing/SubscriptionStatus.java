package com.codezilla.crm.billing;

public enum SubscriptionStatus {
    /** Default state for FREE plan or active paid plan. */
    ACTIVE,
    /** Stripe states we relay back as-is. */
    TRIALING,
    PAST_DUE,
    CANCELED,
    INCOMPLETE,
    UNPAID;

    public static SubscriptionStatus fromStripe(String s) {
        if (s == null) return ACTIVE;
        return switch (s.toLowerCase()) {
            case "active" -> ACTIVE;
            case "trialing" -> TRIALING;
            case "past_due" -> PAST_DUE;
            case "canceled", "cancelled" -> CANCELED;
            case "incomplete", "incomplete_expired" -> INCOMPLETE;
            case "unpaid" -> UNPAID;
            default -> ACTIVE;
        };
    }
}
