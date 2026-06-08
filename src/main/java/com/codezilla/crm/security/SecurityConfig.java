package com.codezilla.crm.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * OWNER > ADMIN > AGENT > VIEWER. Higher roles implicitly satisfy any
     * lower-role check, so a single `@PreAuthorize("hasRole('ADMIN')")` is
     * satisfied for OWNER too. WEBHOOK is a separate technical role with
     * no relation to the human role tree.
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_OWNER > ROLE_ADMIN
                ROLE_ADMIN > ROLE_AGENT
                ROLE_AGENT > ROLE_VIEWER
                """);
    }

    /** Wires the role hierarchy into @PreAuthorize / @PostAuthorize evaluations. */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler h = new DefaultMethodSecurityExpressionHandler();
        h.setRoleHierarchy(roleHierarchy);
        return h;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthFilter jwt,
                                           WebhookApiKeyFilter webhook,
                                           WebhookRateLimitFilter rateLimit,
                                           LoginRateLimitFilter loginRateLimit,
                                           com.codezilla.crm.apikey.ApiKeyAuthFilter apiKeyAuth,
                                           com.codezilla.crm.webhook.WhatsAppSignatureFilter waSig) throws Exception {
        http
            .csrf(c -> c.disable())
            .cors(c -> {})
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/auth/**", "/actuator/health", "/actuator/health/**", "/actuator/prometheus", "/error").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/webhooks/stripe").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/webhook/whatsapp").permitAll()
                .requestMatchers("/book/**").permitAll()
                .requestMatchers("/webhook/**").hasRole("WEBHOOK")
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/v1/**").authenticated()
                .anyRequest().denyAll())
            .addFilterBefore(rateLimit, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(loginRateLimit, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(webhook, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(waSig, WebhookApiKeyFilter.class)
            .addFilterBefore(apiKeyAuth, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}
