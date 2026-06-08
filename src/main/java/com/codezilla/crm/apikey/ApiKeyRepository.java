package com.codezilla.crm.apikey;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    List<ApiKey> findAllByTenantIdAndUserIdOrderByCreatedAtDesc(UUID tenantId, UUID userId);
    List<ApiKey> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<ApiKey> findByKeyHash(String keyHash);
}
