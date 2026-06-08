package com.codezilla.crm.integration;

import com.codezilla.crm.lead.Lead;

import java.util.List;

public interface SheetsExporter {
    /**
     * Append the given leads to the configured Google Sheet.
     * Returns true if at least one row was written.
     */
    boolean appendLeads(List<Lead> leads);

    boolean isEnabled();
}
