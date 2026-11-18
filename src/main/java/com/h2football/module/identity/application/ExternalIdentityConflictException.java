package com.h2football.module.identity.application;

import com.h2football.module.identity.domain.model.Provider;
import com.h2football.module.identity.domain.model.ResourceType;

public class ExternalIdentityConflictException extends RuntimeException {
  public ExternalIdentityConflictException(
      ResourceType resourceType,
      Provider provider,
      String externalId,
      Long existingCanonicalId,
      Long requestedCanonicalId) {
    super(
        "External identity "
            + resourceType
            + "/"
            + provider
            + "/"
            + externalId
            + " is already mapped to canonical ID "
            + existingCanonicalId
            + " and cannot be remapped to "
            + requestedCanonicalId);
  }
}
