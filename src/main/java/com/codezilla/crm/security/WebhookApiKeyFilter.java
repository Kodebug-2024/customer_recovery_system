package com.codezilla.crm.security;

import com.codezilla.crm.tenant.TenantContext;
import com.codezilla.crm.tenant.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates inbound webhook requests via the X-Api-Key header.
 * The key maps to a tenant. We bind that tenant to the request thread so
 * downstream services can persist data tagged with the right tenant.
 */
@Component
public class WebhookApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Api-Key";
    private final TenantRepository tenants;

    public WebhookApiKeyFilter(TenantRepository tenants) {
        this.tenants = tenants;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!request.getRequestURI().startsWith("/webhook/")) return true;
        // Meta verification handshake has no API key.
        return "GET".equalsIgnoreCase(request.getMethod()) && "/webhook/whatsapp".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String apiKey = req.getHeader(HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing API key");
            return;
        }
        var tenant = tenants.findByApiKey(apiKey).orElse(null);
        if (tenant == null) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key");
            return;
        }
        TenantContext.set(tenant.getId());
        var auth = new UsernamePasswordAuthenticationToken(
                "webhook:" + tenant.getId(), null, List.of(new SimpleGrantedAuthority("ROLE_WEBHOOK")));
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
