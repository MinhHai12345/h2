package com.h2football.module.masterdata.domain.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "masterdata_external_identities",
        uniqueConstraints = @UniqueConstraint(name = "uk_masterdata_external_identity",
                columnNames = {"resource_type", "provider", "external_id"}))
public class MasterDataExternalIdentity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "resource_type", nullable = false, length = 30)
    private String resourceType;
    @Column(nullable = false, length = 40)
    private String provider;
    @Column(name = "external_id", nullable = false, length = 120)
    private String externalId;
    @Column(name = "canonical_id", nullable = false)
    private Long canonicalId;
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected MasterDataExternalIdentity() { }

    public MasterDataExternalIdentity(String resourceType, String provider, String externalId, Long canonicalId) {
        this.resourceType = resourceType;
        this.provider = provider;
        this.externalId = externalId;
        this.canonicalId = canonicalId;
    }

    public Long getId() { return id; }
    public String getResourceType() { return resourceType; }
    public String getProvider() { return provider; }
    public String getExternalId() { return externalId; }
    public Long getCanonicalId() { return canonicalId; }
    public Instant getCreatedAt() { return createdAt; }
}
