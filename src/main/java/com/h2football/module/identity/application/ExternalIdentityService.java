package com.h2football.module.identity.application;

import com.h2football.module.identity.domain.model.ExternalIdentity;
import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.identity.domain.model.ResourceType;
import com.h2football.module.identity.domain.repository.ExternalIdentityRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExternalIdentityService {
    private final ExternalIdentityRepository identities;

    public ExternalIdentityService(ExternalIdentityRepository identities) {
        this.identities = identities;
    }

    @Transactional(readOnly = true)
    public Optional<Long> findCanonicalId(
            ResourceType resourceType, Provider provider, String externalId) {
        return identities.findByResourceTypeAndProviderAndExternalId(
                        resourceType, provider, normalize(externalId))
                .map(ExternalIdentity::getCanonicalId);
    }

    public ExternalIdentity register(
            ResourceType resourceType, Provider provider, String externalId, Long canonicalId) {
        if (resourceType == null || provider == null) {
            throw new IllegalArgumentException("resourceType and provider must not be null");
        }
        if (canonicalId == null || canonicalId <= 0) {
            throw new IllegalArgumentException("canonicalId must be positive");
        }

        String normalizedExternalId = normalize(externalId);
        Optional<ExternalIdentity> existing = identities.findByResourceTypeAndProviderAndExternalId(
                resourceType, provider, normalizedExternalId);
        if (existing.isPresent()) {
            ExternalIdentity identity = existing.get();
            if (!identity.getCanonicalId().equals(canonicalId)) {
                throw new ExternalIdentityConflictException(
                        resourceType, provider, normalizedExternalId, identity.getCanonicalId(), canonicalId);
            }
            return identity;
        }

        return identities.save(new ExternalIdentity(
                resourceType, provider, normalizedExternalId, canonicalId));
    }

    private String normalize(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("externalId must not be blank");
        }
        return externalId.trim();
    }
}
