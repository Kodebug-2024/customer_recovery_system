package com.codezilla.crm.lead;

import com.codezilla.crm.audit.AuditEventRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Hard-deletes leads that have been soft-deleted longer than the retention
 * window. Cascades to messages, notes, lead_tags, appointments via FK ON
 * DELETE CASCADE. Runs hourly so the lag between policy and reality is small.
 *
 * Audit rows for deleted leads are kept (we want a record that the deletion
 * happened, even after the lead is gone).
 */
@Component
public class LeadHardDeleteJob {

    private static final Logger log = LoggerFactory.getLogger(LeadHardDeleteJob.class);

    @PersistenceContext private EntityManager em;
    private final AuditEventRepository audit;
    private final long retentionDays;

    public LeadHardDeleteJob(AuditEventRepository audit,
                             @Value("${app.retention.deleted-leads-days:30}") long retentionDays) {
        this.audit = audit;
        this.retentionDays = retentionDays;
    }

    /** Runs every hour at :15 past, so jobs across the cluster don't all fire at once. */
    @Scheduled(cron = "${app.retention.cron:0 15 * * * *}")
    @Transactional
    public void purgeOldSoftDeletes() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        // Native delete: bypasses the @SQLRestriction("deleted_at IS NULL") on Lead so we
        // can actually remove rows that satisfy deleted_at IS NOT NULL.
        int n = em.createNativeQuery("DELETE FROM leads WHERE deleted_at IS NOT NULL AND deleted_at < :cutoff")
                .setParameter("cutoff", Timestamp.from(cutoff))
                .executeUpdate();
        if (n > 0) log.info("Hard-deleted {} leads older than {} days", n, retentionDays);
    }
}
