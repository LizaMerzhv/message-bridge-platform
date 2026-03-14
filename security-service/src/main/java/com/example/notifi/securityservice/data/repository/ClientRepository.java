package com.example.notifi.securityservice.data.repository;

import com.example.notifi.securityservice.data.entity.ClientEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<ClientEntity, UUID> {
  Optional<ClientEntity> findByApiKey(String apiKey);
}
