package com.codezilla.crm.billing;

import com.codezilla.crm.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Enforces plan-based limits. Read-only on the current tenant.
 * Lead-count check uses the start of the current calendar month (UTC).
 */
@Service
public class BillingGate {

    @PersistenceContext private EntityManager em;
    private final SubscriptionRepository subs;

    public BillingGate(SubscriptionRepository subs) {
        this.subs = subs;
    }

    public Plan currentPlan() {
        UUID t = TenantContext.require();
        return subs.findByTenantId(t).map(Subscription::getPlan).orElse(Plan.FREE);
    }

    public boolean aiAllowed() {
        return PlanLimits.aiRepliesAllowed(currentPlan());
    }

    /**
     * @return null if under the cap; otherwise a human-readable reason string.
     */
    @Transactional(readOnly = true)
    public String leadQuotaBlockReason() {
        Plan plan = currentPlan();
        int cap = PlanLimits.leadsPerMonth(plan);
        long used = leadsCreatedThisMonth();
        if (used >= cap) {
            return "Monthly lead limit reached for " + plan
                    + " plan (" + used + "/" + cap + "). Upgrade to continue receiving leads.";
        }
        return null;
    }

    @Transactional(readOnly = true)
    public LeadUsage usage() {
        Plan plan = currentPlan();
        return new LeadUsage(plan, leadsCreatedThisMonth(), PlanLimits.leadsPerMonth(plan));
    }

    public record LeadUsage(Plan plan, long used, int limit) {}

    private long leadsCreatedThisMonth() {
        UUID t = TenantContext.require();
        var startOfMonth = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1)
                .atStartOfDay().toInstant(ZoneOffset.UTC);
        Object result = em.createNativeQuery("""
                SELECT count(*) FROM leads
                 WHERE tenant_id = :t
                   AND created_at >= :from
                """)
                .setParameter("t", t)
                .setParameter("from", Timestamp.from(startOfMonth))
                .getSingleResult();
        return ((Number) result).longValue();
    }
}
