package com.codezilla.crm.billing;

import com.codezilla.crm.testsupport.RedisMockConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.codezilla.crm.user.User;
import com.codezilla.crm.user.UserRepository;
import com.codezilla.crm.tenant.Tenant;
import com.codezilla.crm.tenant.TenantRepository;
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

@SpringBootTest
@ActiveProfiles("test")
@Import(RedisMockConfig.class)
class BillingStubIntegrationTest {

    @Autowired WebApplicationContext ctx;
    @Autowired ObjectMapper json;
    @Autowired TenantRepository tenants;
    @Autowired UserRepository users;
    @Autowired SubscriptionRepository subs;
    @Autowired PasswordEncoder encoder;

    MockMvc mvc;
    String token;
    UUID tenantId;

    @BeforeEach
    void setUp() throws Exception {
        subs.deleteAll();
        users.deleteAll();
        tenants.deleteAll();

        Tenant t = new Tenant();
        t.setName("Acme");
        t.setApiKey("k-" + UUID.randomUUID());
        tenants.save(t);
        tenantId = t.getId();

        User u = new User();
        u.setTenantId(tenantId);
        u.setEmail("admin@acme.test");
        u.setPasswordHash(encoder.encode("password123"));
        u.setRole("OWNER");
        u.setEnabled(true);
        users.save(u);

        mvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
        String resp = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@acme.test\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = json.readTree(resp).get("token").asText();
    }

    private String bearer() { return "Bearer " + token; }

    @Test
    void initial_plan_is_FREE() throws Exception {
        String resp = mvc.perform(get("/api/billing").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode b = json.readTree(resp);
        assertThat(b.get("plan").asText()).isEqualTo("FREE");
        assertThat(b.get("live").asBoolean()).isFalse();
    }

    @Test
    void stub_checkout_upgrades_tenant_to_PRO() throws Exception {
        mvc.perform(post("/api/billing/checkout")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"PRO\"}"))
                .andExpect(status().isOk());

        String after = mvc.perform(get("/api/billing").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(after).get("plan").asText()).isEqualTo("PRO");
    }

    @Test
    void plans_endpoint_returns_catalog() throws Exception {
        String resp = mvc.perform(get("/api/billing/plans").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = json.readTree(resp);
        assertThat(root.get("free")).isNotNull();
        assertThat(root.get("pro").get("priceMonthly").asInt()).isPositive();
    }
}
