package com.codezilla.crm.lead;

import com.codezilla.crm.tenant.TenantContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final LeadRepository leads;

    public StatsController(LeadRepository leads) {
        this.leads = leads;
    }

    public record Stats(long total, long active, long won, long lost, double conversionRate) {}

    @GetMapping
    public Stats summary() {
        UUID t = TenantContext.require();
        long total = leads.findAllByTenantId(t).size();
        long won = leads.countByTenantIdAndStatus(t, LeadStatus.WON);
        long lost = leads.countByTenantIdAndStatus(t, LeadStatus.LOST);
        long active = total - won - lost;
        double rate = total == 0 ? 0.0 : (won * 1.0) / total;
        return new Stats(total, active, won, lost, rate);
    }
}
