package com.example.notifi.api.data.repository;

import com.example.notifi.api.data.entity.ClientEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<ClientEntity, UUID> {
    Optional<ClientEntity> findByApiKey(String apiKey);
}
