package com.codezilla.crm.lead;

import com.codezilla.crm.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregate read-only endpoints for the dashboard charts.
 * Queries are tenant-scoped via TenantContext.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @PersistenceContext
    private EntityManager em;

    public record DailyCount(String date, long count) {}
    public record SourceCount(String source, long count) {}
    public record StatusCount(String status, long count) {}

    /** Lead creations per day for the last {days} days (defaults 30). Zero-fills missing days. */
    @GetMapping("/leads-per-day")
    @SuppressWarnings("unchecked")
    public List<DailyCount> leadsPerDay(@RequestParam(defaultValue = "30") int days) {
        days = Math.max(1, Math.min(days, 180));
        Instant from = LocalDate.now(ZoneOffset.UTC).minusDays(days - 1).atStartOfDay().toInstant(ZoneOffset.UTC);

        var rows = em.createNativeQuery("""
                SELECT to_char(date_trunc('day', created_at), 'YYYY-MM-DD') AS d,
                       count(*) AS c
                  FROM leads
                 WHERE tenant_id = :t
                   AND deleted_at IS NULL
                   AND created_at >= :from
                 GROUP BY 1
                 ORDER BY 1
                """)
                .setParameter("t", TenantContext.require())
                .setParameter("from", Timestamp.from(from))
                .getResultList();

        Map<String, Long> byDate = new HashMap<>();
        for (Object row : rows) {
            Object[] cols = (Object[]) row;
            byDate.put(cols[0].toString(), ((Number) cols[1]).longValue());
        }

        // Zero-fill so the chart x-axis is continuous.
        List<DailyCount> out = new ArrayList<>(days);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int i = days - 1; i >= 0; i--) {
            String key = today.minusDays(i).toString();
            out.add(new DailyCount(key, byDate.getOrDefault(key, 0L)));
        }
        return out;
    }

    /** Lead count per source. */
    @GetMapping("/leads-by-source")
    @SuppressWarnings("unchecked")
    public List<SourceCount> leadsBySource() {
        var rows = em.createNativeQuery("""
                SELECT source, count(*) FROM leads
                 WHERE tenant_id = :t AND deleted_at IS NULL
                 GROUP BY source
                 ORDER BY 2 DESC
                """)
                .setParameter("t", TenantContext.require())
                .getResultList();
        List<SourceCount> out = new ArrayList<>();
        for (Object row : rows) {
            Object[] cols = (Object[]) row;
            out.add(new SourceCount((String) cols[0], ((Number) cols[1]).longValue()));
        }
        return out;
    }

    /** Lead count per status for funnel display. */
    @GetMapping("/leads-by-status")
    @SuppressWarnings("unchecked")
    public List<StatusCount> leadsByStatus() {
        var rows = em.createNativeQuery("""
                SELECT status, count(*) FROM leads
                 WHERE tenant_id = :t AND deleted_at IS NULL
                 GROUP BY status
                """)
                .setParameter("t", TenantContext.require())
                .getResultList();
        Map<String, Long> map = new HashMap<>();
        for (Object row : rows) {
            Object[] cols = (Object[]) row;
            map.put((String) cols[0], ((Number) cols[1]).longValue());
        }
        // Return in canonical funnel order so the chart is intuitive.
        List<StatusCount> out = new ArrayList<>();
        for (String s : List.of("NEW", "CONTACTED", "QUALIFIED", "WON", "LOST")) {
            out.add(new StatusCount(s, map.getOrDefault(s, 0L)));
        }
        return out;
    }
}
