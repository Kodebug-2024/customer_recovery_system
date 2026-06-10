package com.codezilla.crm.webhook;

import com.codezilla.crm.tenant.Tenant;
import com.codezilla.crm.tenant.TenantContext;
import com.codezilla.crm.tenant.TenantRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Authenticates and routes Meta WhatsApp Cloud API webhook calls. The flow:
 *
 * <ol>
 *   <li>GET /webhook/whatsapp — verification handshake; no body, no signature.
 *       Auth happens later in the controller against {@code WHATSAPP_VERIFY_TOKEN}.</li>
 *   <li>POST /webhook/whatsapp — Meta sends a JSON envelope. We:
 *     <ul>
 *       <li>Parse the body to extract {@code metadata.phone_number_id}.</li>
 *       <li>Look up the tenant by that phone-number-id and bind {@link TenantContext}.</li>
 *       <li>If that tenant has a {@code webhook_secret}, verify {@code X-Hub-Signature-256}.</li>
 *       <li>If no tenant matches the phone-number-id, ACK with 200 (Meta would
 *           otherwise retry indefinitely; we just have nowhere to file the message).</li>
 *     </ul>
 *   </li>
 * </ol>
 */
@Component
public class WhatsAppSignatureFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppSignatureFilter.class);
    private static final String SIG_HEADER = "X-Hub-Signature-256";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final TenantRepository tenants;

    public WhatsAppSignatureFilter(TenantRepository tenants) {
        this.tenants = tenants;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only run for the POST callback. The GET handshake is auth'd in the controller.
        return !("/webhook/whatsapp".equals(request.getRequestURI())
                && "POST".equalsIgnoreCase(request.getMethod()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        CachedBodyHttpServletRequest cached = new CachedBodyHttpServletRequest(req);

        String phoneNumberId = extractPhoneNumberId(cached.body());
        if (phoneNumberId == null) {
            log.warn("WhatsApp webhook payload missing metadata.phone_number_id; ignoring");
            ack(res);
            return;
        }

        Optional<Tenant> tenantOpt = tenants.findByWhatsappPhoneNumberId(phoneNumberId);
        if (tenantOpt.isEmpty()) {
            log.warn("WhatsApp webhook: no tenant configured for phone_number_id={}", phoneNumberId);
            ack(res);
            return;
        }
        Tenant tenant = tenantOpt.get();

        String secret = tenant.getWebhookSecret();
        if (secret != null && !secret.isBlank()) {
            String header = req.getHeader(SIG_HEADER);
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

        TenantContext.set(tenant.getId());
        var auth = new UsernamePasswordAuthenticationToken(
                "webhook:" + tenant.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_WEBHOOK")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            chain.doFilter(cached, res);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    /** ACK Meta with 200 so it doesn't retry — we deliberately accept-and-ignore. */
    private static void ack(HttpServletResponse res) throws IOException {
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("application/json");
        res.getWriter().write("{\"status\":\"ignored\"}");
    }

    /** Walks entry[0].changes[0].value.metadata.phone_number_id. Returns null if absent. */
    private static String extractPhoneNumberId(byte[] body) {
        try {
            JsonNode root = JSON.readTree(body);
            JsonNode node = root.path("entry").path(0).path("changes").path(0)
                    .path("value").path("metadata").path("phone_number_id");
            return node.isMissingNode() || node.isNull() ? null : node.asText();
        } catch (Exception e) {
            return null;
        }
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
