package com.h2football.module.identity.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "external_identities",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_external_identity_source",
            columnNames = {"resource_type", "provider", "external_id"}))
public class ExternalIdentity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "resource_type", nullable = false, length = 30)
  private ResourceType resourceType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private Provider provider;

  @Column(name = "external_id", nullable = false, length = 120)
  private String externalId;

  @Column(name = "canonical_id", nullable = false)
  private Long canonicalId;

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private Instant createdAt;

  protected ExternalIdentity() {}

  public ExternalIdentity(
      ResourceType resourceType, Provider provider, String externalId, Long canonicalId) {
    this.resourceType = resourceType;
    this.provider = provider;
    this.externalId = externalId;
    this.canonicalId = canonicalId;
  }

  public Long getId() {
    return id;
  }

  public ResourceType getResourceType() {
    return resourceType;
  }

  public Provider getProvider() {
    return provider;
  }

  public String getExternalId() {
    return externalId;
  }

  public Long getCanonicalId() {
    return canonicalId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
