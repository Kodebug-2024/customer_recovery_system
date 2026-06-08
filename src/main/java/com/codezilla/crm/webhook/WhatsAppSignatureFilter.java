package com.codezilla.crm.webhook;

import com.codezilla.crm.tenant.TenantContext;
import com.codezilla.crm.tenant.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

/**
 * For /webhook/whatsapp: if the tenant has a webhook_secret configured, verify
 * the X-Hub-Signature-256 header (HMAC-SHA256 of the raw body) before passing
 * the request to the controller. Wraps the request so the body can be re-read.
 */
@Component
public class WhatsAppSignatureFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Hub-Signature-256";
    private final TenantRepository tenants;

    public WhatsAppSignatureFilter(TenantRepository tenants) {
        this.tenants = tenants;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"/webhook/whatsapp".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        UUID tenantId = TenantContext.get();
        String secret = tenantId == null ? null
                : tenants.findById(tenantId).map(t -> t.getWebhookSecret()).orElse(null);

        CachedBodyHttpServletRequest cached = new CachedBodyHttpServletRequest(req);

        if (secret != null && !secret.isBlank()) {
            String header = req.getHeader(HEADER);
            if (header == null || !header.startsWith("sha256=")) {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing signature");
                return;
            }
            String expected = "sha256=" + hmacSha256(secret, cached.body());
            if (!constantTimeEquals(expected, header)) {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Bad signature");
                return;
            }
        }
        chain.doFilter(cached, res);
    }

    private static String hmacSha256(String secret, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failed", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}
