# Master Data

The masterdata module owns canonical countries, leagues, seasons and teams. Provider-specific identifiers are stored separately from canonical IDs in `masterdata_external_identities`; provider payloads must not become canonical entities directly.

## Season convention

- `season_code` is the stable machine-readable key (for example `2025` or `2025-2026`).
- `display_name` is the human-readable label (for example `2025/2026`).
- `start_date` and `end_date` represent the actual competition period.
- Season uniqueness is scoped to a league, not global.

## External identities

The `MasterDataExternalIdentity` entity maps a provider resource to its canonical record using the unique key:

`resource_type + provider + external_id`

The canonical ID remains independent of the provider ID. Import operations first resolve an existing identity, then reuse a natural-key match where possible, and only then create a canonical record. Provider adapters must call the masterdata import application service; they must not write canonical repositories directly.

## API

- `POST/GET /api/v1/master-data/countries`
- `POST/GET /api/v1/master-data/leagues`
- `POST/GET /api/v1/master-data/seasons` (optional `leagueId` filter)
- `POST/GET /api/v1/master-data/teams`

All writes are validated in API/application layers and protected by database constraints. Flyway owns schema creation; Hibernate remains in validate mode.

## Follow-up boundary

Provider identity resolution and ingestion workflows use the external identity foundation without coupling provider DTOs to canonical entities. Season records must not be imported until the source supplies trustworthy competition start and end dates.
