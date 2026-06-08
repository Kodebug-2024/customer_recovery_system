package com.codezilla.crm.user;

import com.codezilla.crm.audit.AuditService;
import com.codezilla.crm.tenant.TenantContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public UserService(UserRepository users, PasswordEncoder encoder, AuditService audit) {
        this.users = users;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<User> list() {
        return users.findAllByTenantIdOrderByEmailAsc(TenantContext.require());
    }

    @Transactional(readOnly = true)
    public User getInTenant(UUID id) {
        User u = users.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "user not found"));
        if (!u.getTenantId().equals(TenantContext.require())) {
            throw new ResponseStatusException(NOT_FOUND, "user not found");
        }
        return u;
    }

    @Transactional
    public User create(String name, String email, String password, String role) {
        if (email == null || email.isBlank()) throw new ResponseStatusException(BAD_REQUEST, "email required");
        if (password == null || password.length() < 8) throw new ResponseStatusException(BAD_REQUEST, "password >= 8 chars");
        if (!Roles.isValid(role)) throw new ResponseStatusException(BAD_REQUEST, "invalid role");
        if (users.existsByEmail(email.toLowerCase())) throw new ResponseStatusException(CONFLICT, "email already used");

        User u = new User();
        u.setTenantId(TenantContext.require());
        u.setName(name);
        u.setEmail(email.toLowerCase());
        u.setPasswordHash(encoder.encode(password));
        u.setRole(role);
        u.setEnabled(true);
        users.save(u);
        audit.record("user", u.getId(), "CREATE", "email=" + u.getEmail() + " role=" + role);
        return u;
    }

    @Transactional
    public User updateRole(UUID id, String newRole) {
        if (!Roles.isValid(newRole)) throw new ResponseStatusException(BAD_REQUEST, "invalid role");
        User u = getInTenant(id);
        ensureNotLastEnabledAdmin(u, newRole, u.isEnabled());
        String old = u.getRole();
        u.setRole(newRole);
        audit.record("user", id, "ROLE_CHANGE", old + " -> " + newRole);
        return u;
    }

    @Transactional
    public User setEnabled(UUID id, boolean enabled) {
        User u = getInTenant(id);
        ensureNotLastEnabledAdmin(u, u.getRole(), enabled);
        u.setEnabled(enabled);
        audit.record("user", id, enabled ? "ENABLE" : "DISABLE", null);
        return u;
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 8)
            throw new ResponseStatusException(BAD_REQUEST, "password >= 8 chars");
        User u = users.findById(userId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "user not found"));
        if (!encoder.matches(currentPassword, u.getPasswordHash()))
            throw new ResponseStatusException(BAD_REQUEST, "current password incorrect");
        u.setPasswordHash(encoder.encode(newPassword));
        audit.record("user", userId, "PASSWORD_CHANGE", null);
    }

    @Transactional
    public void resetPassword(UUID id, String newPassword) {
        if (newPassword == null || newPassword.length() < 8)
            throw new ResponseStatusException(BAD_REQUEST, "password >= 8 chars");
        User u = getInTenant(id);
        u.setPasswordHash(encoder.encode(newPassword));
        audit.record("user", id, "PASSWORD_RESET", null);
    }

    @Transactional
    public void recordLogin(UUID userId) {
        users.findById(userId).ifPresent(u -> u.setLastLoginAt(java.time.Instant.now()));
    }

    /** Prevent locking the tenant out of admin access. */
    private void ensureNotLastEnabledAdmin(User target, String newRole, boolean newEnabled) {
        boolean wasAdmin = Roles.ADMIN.equals(target.getRole()) || Roles.OWNER.equals(target.getRole());
        boolean willBeAdmin = (Roles.ADMIN.equals(newRole) || Roles.OWNER.equals(newRole)) && newEnabled;
        if (wasAdmin && !willBeAdmin) {
            long otherAdmins = users.countByTenantIdAndRoleAndEnabled(target.getTenantId(), Roles.ADMIN, true)
                    + users.countByTenantIdAndRoleAndEnabled(target.getTenantId(), Roles.OWNER, true);
            // subtract self if currently counted as enabled admin
            if (target.isEnabled() && wasAdmin) otherAdmins -= 1;
            if (otherAdmins <= 0) {
                throw new ResponseStatusException(BAD_REQUEST, "cannot remove the last enabled admin");
            }
        }
    }
}
