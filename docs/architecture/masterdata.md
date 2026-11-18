# Master Data

The masterdata module owns canonical countries, leagues, seasons and teams. Provider-specific identifiers are managed by the shared `identity` module and stored separately from canonical IDs in `external_identities`.

## Season convention

- `season_code` is the stable machine-readable key (for example `2025` or `2025-2026`).
- `display_name` is the human-readable label (for example `2025/2026`).
- `start_date` and `end_date` represent the actual competition period.
- Season uniqueness is scoped to a league, not global.

## Import boundary

Provider adapters must not write canonical repositories directly. The ingestion application service calls the masterdata import application service, which resolves natural keys and delegates provider identity registration to the shared identity module.

## API

- `POST/GET /api/v1/master-data/countries`
- `POST/GET /api/v1/master-data/leagues`
- `POST/GET /api/v1/master-data/seasons` (optional `leagueId` filter)
- `POST/GET /api/v1/master-data/teams`

All writes are validated in API/application layers and protected by database constraints. Flyway owns schema creation; Hibernate remains in validate mode.
