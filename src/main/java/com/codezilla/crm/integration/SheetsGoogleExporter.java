package com.codezilla.crm.integration;

import com.codezilla.crm.lead.Lead;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Real Google Sheets exporter. Uses a service account JSON to authenticate
 * and appends rows to a target spreadsheet. Share the sheet with the service
 * account email before use.
 */
@Component
@ConditionalOnProperty(name = "integrations.sheets.mode", havingValue = "real")
public class SheetsGoogleExporter implements SheetsExporter {

    private static final Logger log = LoggerFactory.getLogger(SheetsGoogleExporter.class);

    private final String spreadsheetId;
    private final String range;
    private final Sheets sheets;

    public SheetsGoogleExporter(@Value("${integrations.sheets.spreadsheet-id}") String spreadsheetId,
                                @Value("${integrations.sheets.range:Sheet1!A:H}") String range,
                                @Value("${integrations.sheets.credentials-json}") String credentialsJson)
            throws Exception {
        this.spreadsheetId = spreadsheetId;
        this.range = range;
        GoogleCredentials creds = GoogleCredentials
                .fromStream(new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8)))
                .createScoped(Collections.singletonList(SheetsScopes.SPREADSHEETS));
        HttpRequestInitializer init = new HttpCredentialsAdapter(creds);
        this.sheets = new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(), init)
                .setApplicationName("codezilla-crm")
                .build();
    }

    @Override
    public boolean appendLeads(List<Lead> leads) {
        if (leads == null || leads.isEmpty()) return false;
        List<List<Object>> rows = new ArrayList<>();
        for (Lead l : leads) {
            rows.add(List.of(
                    s(l.getId()),
                    s(l.getName()),
                    s(l.getPhone()),
                    s(l.getEmail()),
                    s(l.getSource()),
                    s(l.getStatus()),
                    s(l.getMessage()),
                    s(l.getCreatedAt())));
        }
        try {
            sheets.spreadsheets().values()
                    .append(spreadsheetId, range, new ValueRange().setValues(rows))
                    .setValueInputOption("RAW")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute();
            log.info("Appended {} rows to spreadsheet {}", rows.size(), spreadsheetId);
            return true;
        } catch (Exception e) {
            log.error("Sheets append failed", e);
            return false;
        }
    }

    @Override
    public boolean isEnabled() { return true; }

    private static String s(Object o) { return o == null ? "" : o.toString(); }
}
