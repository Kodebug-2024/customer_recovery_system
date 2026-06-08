package com.codezilla.crm.integration;

import com.codezilla.crm.lead.Lead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "integrations.sheets.mode", havingValue = "stub", matchIfMissing = true)
public class SheetsStubExporter implements SheetsExporter {
    private static final Logger log = LoggerFactory.getLogger(SheetsStubExporter.class);

    @Override
    public boolean appendLeads(List<Lead> leads) {
        log.info("[STUB SHEETS] would append {} leads", leads.size());
        return false;
    }

    @Override
    public boolean isEnabled() { return false; }
}
