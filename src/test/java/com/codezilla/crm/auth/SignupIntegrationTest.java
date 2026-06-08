package com.codezilla.crm.auth;

import com.codezilla.crm.testsupport.RedisMockConfig;
import com.codezilla.crm.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(RedisMockConfig.class)
class SignupIntegrationTest {

    @Autowired WebApplicationContext ctx;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        mvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
    }

    @Test
    void register_creates_tenant_and_owner_and_returns_jwt() throws Exception {
        String body = """
            {"businessName":"Acme","industry":"Retail","name":"Owner","email":"owner@acme.test","password":"hunter2hunter2"}
            """;

        String resp = mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = json.readTree(resp);
        assertThat(root.get("token").asText()).isNotBlank();
        assertThat(root.get("user").get("role").asText()).isEqualTo("OWNER");
        assertThat(root.get("user").get("email").asText()).isEqualTo("owner@acme.test");

        // Token works for subsequent calls.
        String token = root.get("token").asText();
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"owner@acme.test\",\"password\":\"hunter2hunter2\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void register_rejects_duplicate_email() throws Exception {
        String body = """
            {"businessName":"Acme","email":"dup@x.com","password":"hunter2hunter2"}
            """;
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void register_rejects_short_password() throws Exception {
        String body = """
            {"businessName":"Acme","email":"short@x.com","password":"short"}
            """;
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void owner_can_access_admin_only_endpoints() throws Exception {
        String body = """
            {"businessName":"Owner Co","email":"boss@x.com","password":"hunter2hunter2"}
            """;
        String resp = mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(resp).get("token").asText();

        // /api/users requires ADMIN; OWNER must satisfy it via role hierarchy.
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/settings").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/audit").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void account_locks_after_repeated_failed_logins() throws Exception {
        // Register first.
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessName\":\"L\",\"email\":\"locker@x.com\",\"password\":\"goodpass1\"}"))
                .andExpect(status().isCreated());

        String wrong = "{\"email\":\"locker@x.com\",\"password\":\"WRONG_____\"}";
        // 5 wrong attempts → next attempt should be 423 LOCKED.
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON).content(wrong))
                    .andExpect(status().isUnauthorized());
        }
        // Even with the correct password, account is locked.
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"locker@x.com\",\"password\":\"goodpass1\"}"))
                .andExpect(status().isLocked());
    }

    @Test
    void email_verification_consume_marks_user_verified() throws Exception {
        String reg = mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessName\":\"V\",\"email\":\"verify@x.com\",\"password\":\"goodpass1\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(reg).get("user").get("emailVerified").asBoolean()).isFalse();

        // Pull the token straight from the DB (in real flow it arrives by email).
        com.codezilla.crm.user.User u = users.findByEmail("verify@x.com").orElseThrow();
        String token = u.getEmailVerificationToken();
        assertThat(token).isNotBlank();

        mvc.perform(post("/auth/verify-email").param("token", token))
                .andExpect(status().isOk());

        com.codezilla.crm.user.User after = users.findByEmail("verify@x.com").orElseThrow();
        assertThat(after.getEmailVerifiedAt()).isNotNull();
        assertThat(after.getEmailVerificationToken()).isNull();
    }

    @Test
    void verify_email_with_bad_token_fails() throws Exception {
        mvc.perform(post("/auth/verify-email").param("token", "not-a-real-token"))
                .andExpect(status().isBadRequest());
    }
}
