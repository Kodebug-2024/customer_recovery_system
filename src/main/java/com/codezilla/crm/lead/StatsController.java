package com.codezilla.crm.lead;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        long total = leads.count();
        long won = leads.countByStatus(LeadStatus.WON);
        long lost = leads.countByStatus(LeadStatus.LOST);
        long active = total - won - lost;
        double rate = total == 0 ? 0.0 : (won * 1.0) / total;
        return new Stats(total, active, won, lost, rate);
    }
}
