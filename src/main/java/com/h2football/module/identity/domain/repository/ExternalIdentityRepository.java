package com.h2football.module.identity.domain.repository;

import com.h2football.module.identity.domain.model.ExternalIdentity;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.identity.domain.model.ResourceType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentity, Long> {
  Optional<ExternalIdentity> findByResourceTypeAndProviderAndExternalId(
      ResourceType resourceType, Provider provider, String externalId);
}
