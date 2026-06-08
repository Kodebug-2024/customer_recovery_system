package com.codezilla.crm.tenant;

import com.codezilla.crm.testsupport.RedisMockConfig;
import com.codezilla.crm.user.User;
import com.codezilla.crm.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies that tenant A and tenant B cannot read or modify each other's data.
 * Covers leads, users, settings, and audit. Hibernate's @Filter does not apply
 * to find-by-PK, so this also catches regressions in service-layer tenant checks.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(RedisMockConfig.class)
class TenantIsolationIntegrationTest {

    @Autowired WebApplicationContext ctx;
    @Autowired ObjectMapper json;
    @Autowired TenantRepository tenants;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    MockMvc mvc;

    UUID tenantA;
    UUID tenantB;
    UUID userAId;
    String tokenA;
    String tokenB;

    @BeforeEach
    void setUp() throws Exception {
        users.deleteAll();
        tenants.deleteAll();

        Tenant ta = new Tenant();
        ta.setName("Tenant A");
        ta.setApiKey("key-a-" + UUID.randomUUID());
        tenants.save(ta);
        tenantA = ta.getId();

        Tenant tb = new Tenant();
        tb.setName("Tenant B");
        tb.setApiKey("key-b-" + UUID.randomUUID());
        tenants.save(tb);
        tenantB = tb.getId();

        User ua = newUser(tenantA, "a@test.local", "ADMIN");
        users.save(ua);
        userAId = ua.getId();

        User ub = newUser(tenantB, "b@test.local", "ADMIN");
        users.save(ub);

        mvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
        tokenA = login("a@test.local", "pw12345678");
        tokenB = login("b@test.local", "pw12345678");
    }

    private User newUser(UUID tenantId, String email, String role) {
        User u = new User();
        u.setTenantId(tenantId);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode("pw12345678"));
        u.setRole(role);
        u.setEnabled(true);
        return u;
    }

    private String login(String email, String pw) throws Exception {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + pw + "\"}";
        String resp = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("token").asText();
    }

    private String bearer(String token) { return "Bearer " + token; }

    private String createLeadAs(String token, String name) throws Exception {
        String body = "{\"name\":\"" + name + "\",\"phone\":\"+10000000\",\"source\":\"web\"}";
        String resp = mvc.perform(post("/api/leads")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("id").asText();
    }

    // ----- Leads -----

    @Test
    void tenantB_cannot_list_tenantA_leads() throws Exception {
        createLeadAs(tokenA, "Alice");

        String resp = mvc.perform(get("/api/leads")
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode page = json.readTree(resp);
        assertThat(page.get("content")).hasSize(0);
        assertThat(page.get("totalElements").asInt()).isZero();
    }

    @Test
    void tenantB_cannot_get_tenantA_lead_by_id() throws Exception {
        String leadId = createLeadAs(tokenA, "Alice");

        // This is the @Filter bypass that find-by-PK exposes. Service must enforce.
        mvc.perform(get("/api/leads/" + leadId)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());

        // Sanity check: tenant A still sees their own.
        mvc.perform(get("/api/leads/" + leadId)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk());
    }

    @Test
    void tenantB_cannot_delete_tenantA_lead() throws Exception {
        String leadId = createLeadAs(tokenA, "Alice");

        mvc.perform(delete("/api/leads/" + leadId)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());

        // Still exists for tenant A.
        mvc.perform(get("/api/leads/" + leadId)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk());
    }

    @Test
    void tenantB_cannot_change_tenantA_lead_status() throws Exception {
        String leadId = createLeadAs(tokenA, "Alice");

        mvc.perform(patch("/api/leads/" + leadId + "/status")
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"LOST\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void tenantB_cannot_post_message_to_tenantA_lead() throws Exception {
        String leadId = createLeadAs(tokenA, "Alice");

        mvc.perform(post("/api/leads/" + leadId + "/messages")
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hi\"}"))
                .andExpect(status().isNotFound());
    }

    // ----- Users -----

    @Test
    void tenantB_users_list_does_not_contain_tenantA_users() throws Exception {
        String resp = mvc.perform(get("/api/users")
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(resp).contains("b@test.local").doesNotContain("a@test.local");
    }

    @Test
    void tenantB_cannot_change_tenantA_user_role() throws Exception {
        mvc.perform(patch("/api/users/" + userAId + "/role")
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"VIEWER\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void tenantB_cannot_disable_tenantA_user() throws Exception {
        mvc.perform(patch("/api/users/" + userAId + "/enabled")
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void tenantB_cannot_reset_tenantA_user_password() throws Exception {
        mvc.perform(post("/api/users/" + userAId + "/reset-password")
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"hijacked1\"}"))
                .andExpect(status().isNotFound());
    }

    // ----- Settings -----

    @Test
    void settings_are_isolated_per_tenant() throws Exception {
        mvc.perform(put("/api/settings")
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tenant A renamed\",\"webhookSecret\":\"secret-a\"}"))
                .andExpect(status().isOk());

        String respB = mvc.perform(get("/api/settings")
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode b = json.readTree(respB);
        assertThat(b.get("name").asText()).isEqualTo("Tenant B");
        assertThat(b.get("webhookSecretConfigured").asBoolean()).isFalse();
    }

    @Test
    void per_tenant_integration_credentials_are_isolated_and_never_returned_in_plaintext() throws Exception {
        // Tenant A sets WhatsApp + OpenAI + Telegram credentials.
        String secretToken = "EAAOyZ_super_secret_whatsapp_token_for_A";
        String openAiKey = "sk-supersecret-openai-A";
        mvc.perform(put("/api/settings")
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"whatsappPhoneNumberId\":\"1234567890\","
                                + "\"whatsappAccessToken\":\"" + secretToken + "\","
                                + "\"telegramBotToken\":\"tg-bot-token-A\","
                                + "\"openaiApiKey\":\"" + openAiKey + "\"}"))
                .andExpect(status().isOk());

        // GET for tenant A: must show "configured" booleans, NOT the plaintext token.
        String respA = mvc.perform(get("/api/settings")
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(respA).contains("\"whatsappAccessTokenConfigured\":true");
        assertThat(respA).contains("\"openaiApiKeyConfigured\":true");
        assertThat(respA).contains("\"telegramBotTokenConfigured\":true");
        assertThat(respA).doesNotContain(secretToken);
        assertThat(respA).doesNotContain(openAiKey);

        // GET for tenant B: must not see any of tenant A's credentials.
        String respB = mvc.perform(get("/api/settings")
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(respB).contains("\"whatsappAccessTokenConfigured\":false");
        assertThat(respB).contains("\"openaiApiKeyConfigured\":false");
        assertThat(respB).contains("\"telegramBotTokenConfigured\":false");
        assertThat(respB).doesNotContain(secretToken);
    }

    @Test
    void clearing_a_credential_removes_it() throws Exception {
        mvc.perform(put("/api/settings")
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"whatsappAccessToken\":\"set-then-cleared\"}"))
                .andExpect(status().isOk());

        // Empty string == clear.
        mvc.perform(put("/api/settings")
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"whatsappAccessToken\":\"\"}"))
                .andExpect(status().isOk());

        String resp = mvc.perform(get("/api/settings")
                        .header("Authorization", bearer(tokenA)))
                .andReturn().getResponse().getContentAsString();
        assertThat(resp).contains("\"whatsappAccessTokenConfigured\":false");
    }

    // ----- Audit log -----

    @Test
    void tenantB_audit_log_does_not_contain_tenantA_events() throws Exception {
        // Generate an event for tenant A.
        createLeadAs(tokenA, "Alice");

        String respB = mvc.perform(get("/api/audit?size=50")
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode page = json.readTree(respB);
        assertThat(page.get("totalElements").asInt())
                .as("tenant B should see no audit entries from tenant A")
                .isZero();

        // Tenant A sees their own.
        String respA = mvc.perform(get("/api/audit?size=50")
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(respA).get("totalElements").asInt()).isPositive();
    }
}
