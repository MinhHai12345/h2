# Master Data

The masterdata module owns canonical countries, leagues, seasons and teams. Provider-specific identifiers are stored separately from canonical IDs in `masterdata_external_identities`; provider payloads must not become canonical entities directly.

## Season convention

- `season_code` is the stable machine-readable key (for example `2025` or `2025-2026`).
- `display_name` is the human-readable label (for example `2025/2026`).
- `start_date` and `end_date` represent the actual competition period.
- Season uniqueness is scoped to a league, not global.

## API

- `POST/GET /api/v1/master-data/countries`
- `POST/GET /api/v1/master-data/leagues`
- `POST/GET /api/v1/master-data/seasons` (optional `leagueId` filter)
- `POST/GET /api/v1/master-data/teams`

All writes are validated in API/application layers and protected by database constraints. Flyway owns schema creation; Hibernate remains in validate mode.

## Follow-up boundary

The external identity table is the persistence foundation for provider mappings. Provider-specific identity resolution and ingestion workflows will be implemented in the provider/ingestion phase, rather than coupling provider DTOs to these canonical entities.
