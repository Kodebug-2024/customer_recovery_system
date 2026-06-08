package com.codezilla.crm.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailVerificationToken(String token);
    List<User> findAllByTenantIdOrderByEmailAsc(UUID tenantId);
    boolean existsByEmail(String email);
    long countByTenantIdAndRoleAndEnabled(UUID tenantId, String role, boolean enabled);
}
