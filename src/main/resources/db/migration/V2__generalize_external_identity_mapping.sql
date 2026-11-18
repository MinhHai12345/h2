RENAME TABLE masterdata_external_identities TO external_identities;

ALTER TABLE external_identities
    RENAME INDEX uk_masterdata_external_identity TO uk_external_identity_source,
    RENAME INDEX idx_masterdata_canonical_identity TO idx_external_identity_canonical;
