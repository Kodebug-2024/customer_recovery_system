package com.codezilla.crm.lead;

import com.codezilla.crm.audit.AuditService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leads")
public class LeadCsvController {

    private static final List<String> HEADERS =
            List.of("id", "name", "phone", "email", "source", "status", "message", "created_at");

    private final LeadService leads;
    private final LeadRepository repo;
    private final AuditService audit;

    public LeadCsvController(LeadService leads, LeadRepository repo, AuditService audit) {
        this.leads = leads;
        this.repo = repo;
        this.audit = audit;
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public void export(HttpServletResponse res) throws IOException {
        res.setContentType("text/csv; charset=utf-8");
        res.setHeader("Content-Disposition", "attachment; filename=\"leads.csv\"");
        try (PrintWriter w = res.getWriter()) {
            w.println(String.join(",", HEADERS));
            for (Lead l : repo.findAll()) {
                w.println(String.join(",",
                        csv(l.getId().toString()),
                        csv(l.getName()),
                        csv(l.getPhone()),
                        csv(l.getEmail()),
                        csv(l.getSource()),
                        csv(l.getStatus().name()),
                        csv(l.getMessage()),
                        csv(l.getCreatedAt() == null ? "" : l.getCreatedAt().toString())));
            }
        }
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        int created = 0;
        int skipped = 0;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = r.readLine();
            if (header == null) return Map.of("created", 0, "skipped", 0);
            String[] cols = parseLine(header);
            Map<String, Integer> idx = new HashMap<>();
            for (int i = 0; i < cols.length; i++) idx.put(cols[i].trim().toLowerCase(), i);
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] v = parseLine(line);
                try {
                    LeadRequest req = new LeadRequest(
                            cell(v, idx, "name"),
                            cell(v, idx, "phone"),
                            cell(v, idx, "email"),
                            firstNonNull(cell(v, idx, "source"), "import"),
                            cell(v, idx, "message"));
                    leads.create(req);
                    created++;
                } catch (Exception ex) {
                    skipped++;
                }
            }
        }
        audit.record("lead", new java.util.UUID(0, 0), "IMPORT",
                "created=" + created + " skipped=" + skipped);
        return Map.of("created", created, "skipped", skipped);
    }

    private static String cell(String[] v, Map<String, Integer> idx, String key) {
        Integer i = idx.get(key);
        if (i == null || i >= v.length) return null;
        String s = v[i];
        return (s == null || s.isBlank()) ? null : s;
    }

    private static String firstNonNull(String a, String b) { return a != null ? a : b; }

    private static String csv(String s) {
        if (s == null) return "";
        boolean needsQuote = s.contains(",") || s.contains("\"") || s.contains("\n");
        String escaped = s.replace("\"", "\"\"");
        return needsQuote ? "\"" + escaped + "\"" : escaped;
    }

    // Minimal RFC4180 parser supporting quoted fields with embedded commas.
    private static String[] parseLine(String line) {
        java.util.List<String> out = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else inQuotes = false;
                } else cur.append(c);
            } else {
                if (c == ',') { out.add(cur.toString()); cur.setLength(0); }
                else if (c == '"') inQuotes = true;
                else cur.append(c);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }
}
