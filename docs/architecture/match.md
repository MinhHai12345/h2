# Match domain

The `match` module owns canonical fixtures and results. A match belongs to one canonical season and references distinct home and away teams. Kickoff timestamps are stored as UTC instants; scores remain nullable until the provider reports them.

Match status is normalized into the domain enum and does not expose provider-specific status strings. Provider event identifiers are mapped through the shared `external_identities` table using resource type `MATCH`; no match-specific identity entity is created.

The ingestion flow requires an existing canonical season and existing canonical team mappings. This prevents provider event payloads from creating incomplete master data implicitly. Register the SofaScore season identifier against the selected canonical season before importing events.

## Manual SofaScore sync

`POST /api/v1/ingestion/sofascore/matches/sync?tournamentId={sofaTournamentId}&sofaSeasonId={sofaSeasonId}&canonicalSeasonId={seasonId}&window=NEXT&page=0`

Use `window=NEXT` for upcoming events and `window=LAST` for completed/recent events. Pages are zero-based. The response reports created, updated and skipped event counts with warnings for teams that have not yet been mapped.

List canonical matches with:

`GET /api/v1/matches?seasonId={seasonId}`

The endpoint is protected by the same `X-Ingestion-Admin-Key` requirement as other SofaScore ingestion operations.
