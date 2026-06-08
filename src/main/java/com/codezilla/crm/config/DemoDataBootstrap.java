package com.codezilla.crm.config;

import com.codezilla.crm.user.User;
import com.codezilla.crm.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Provisions the demo admin user on first startup so the dev/demo tenant
 * has a working login with a hash that matches the active PasswordEncoder.
 */
@Component
@Profile("!test")
public class DemoDataBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataBootstrap.class);
    private static final UUID DEMO_TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public DemoDataBootstrap(UserRepository users,
                             PasswordEncoder encoder,
                             @Value("${app.demo.admin-email:admin@demo.local}") String email,
                             @Value("${app.demo.admin-password:password123}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (users.findByEmail(email).isPresent()) return;
        User u = new User();
        u.setTenantId(DEMO_TENANT);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(password));
        u.setRole("ADMIN");
        users.save(u);
        log.info("Provisioned demo admin user: {}", email);
    }
}
