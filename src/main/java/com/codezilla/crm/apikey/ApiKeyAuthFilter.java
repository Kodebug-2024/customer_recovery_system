package com.codezilla.crm.apikey;

import com.codezilla.crm.tenant.TenantContext;
import com.codezilla.crm.user.User;
import com.codezilla.crm.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Authenticates requests to /v1/** (the public REST API) via Bearer API keys.
 * Records last-used timestamp on success. Auth principal is the owning user;
 * tenant context is bound from the key's tenant_id.
 *
 * Note: no @Transactional on the filter — Spring AOP can't proxy
 * OncePerRequestFilter cleanly (its doFilter is final). The repository
 * save() call is itself transactional via SimpleJpaRepository.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiKeyRepository keys;
    private final UserRepository users;

    public ApiKeyAuthFilter(ApiKeyRepository keys, UserRepository users) {
        this.keys = keys;
        this.users = users;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Bearer token");
            return;
        }
        String cleartext = header.substring(7);
        Optional<ApiKey> keyOpt = keys.findByKeyHash(sha256Hex(cleartext));
        if (keyOpt.isEmpty()) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key");
            return;
        }
        ApiKey key = keyOpt.get();
        if (!key.isUsable()) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Revoked or expired API key");
            return;
        }
        User user = users.findById(key.getUserId()).orElse(null);
        if (user == null || !user.isEnabled()) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Owning user is gone or disabled");
            return;
        }

        TenantContext.set(key.getTenantId());
        var auth = new UsernamePasswordAuthenticationToken(
                user.getId(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
        SecurityContextHolder.getContext().setAuthentication(auth);

        key.setLastUsedAt(Instant.now());
        keys.save(key);

        try {
            chain.doFilter(req, res);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
