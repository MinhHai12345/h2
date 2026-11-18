package com.h2football.module.masterdata.domain.repository;

import com.h2football.module.masterdata.domain.model.MasterDataExternalIdentity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasterDataExternalIdentityRepository
        extends JpaRepository<MasterDataExternalIdentity, Long> {
    Optional<MasterDataExternalIdentity> findByResourceTypeAndProviderAndExternalId(
            String resourceType, String provider, String externalId);
}
