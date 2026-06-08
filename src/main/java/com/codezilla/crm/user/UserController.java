package com.codezilla.crm.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;
    private final UserRepository repo;

    public UserController(UserService service, UserRepository repo) {
        this.service = service;
        this.repo = repo;
    }

    public record UserView(UUID id, String name, String email, String role,
                           boolean enabled, Instant lastLoginAt, Instant createdAt) {
        static UserView from(User u) {
            return new UserView(u.getId(), u.getName(), u.getEmail(), u.getRole(),
                    u.isEnabled(), u.getLastLoginAt(), u.getCreatedAt());
        }
    }

    public record CreateRequest(String name, @Email @NotBlank String email,
                                @NotBlank String password, @NotBlank String role) {}
    public record UpdateRoleRequest(@NotBlank String role) {}
    public record EnabledRequest(boolean enabled) {}
    public record ResetPasswordRequest(@NotBlank String password) {}
    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {}

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserView> list() {
        return service.list().stream().map(UserView::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserView create(@RequestBody CreateRequest req) {
        return UserView.from(service.create(req.name(), req.email(), req.password(), req.role()));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserView updateRole(@PathVariable UUID id, @RequestBody UpdateRoleRequest req) {
        return UserView.from(service.updateRole(id, req.role()));
    }

    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public UserView setEnabled(@PathVariable UUID id, @RequestBody EnabledRequest req) {
        return UserView.from(service.setEnabled(id, req.enabled()));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID id, @RequestBody ResetPasswordRequest req) {
        service.resetPassword(id, req.password());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserView me(@AuthenticationPrincipal UUID userId) {
        User u = repo.findById(userId).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
        return UserView.from(u);
    }

    @PostMapping("/me/password")
    public ResponseEntity<Void> changeOwnPassword(@AuthenticationPrincipal UUID userId,
                                                  @RequestBody ChangePasswordRequest req) {
        if (userId == null) throw new ResponseStatusException(UNAUTHORIZED);
        service.changePassword(userId, req.currentPassword(), req.newPassword());
        return ResponseEntity.noContent().build();
    }
}
