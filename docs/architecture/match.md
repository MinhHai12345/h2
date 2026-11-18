# Match domain

The `match` module owns canonical fixtures, results and match-level team statistics. A match belongs to one canonical season and references distinct home and away teams. Kickoff timestamps are stored as UTC instants; scores remain nullable until the provider reports them.

Match status is normalized into the domain enum and does not expose provider-specific status strings. Provider event identifiers are mapped through the shared `external_identities` table using resource type `MATCH`; no match-specific identity entity is created.

The ingestion flow requires an existing canonical season and existing canonical team mappings. This prevents provider event payloads from creating incomplete master data implicitly. Register the SofaScore season identifier against the selected canonical season before importing events.

## Match statistics

Statistics are stored per match, provider, period and statistic key. The schema preserves provider display values and separately stores numeric values when they can be interpreted safely. Values such as `12/26 (46%)` remain intact and are not incorrectly converted into a single numeric measurement.

SofaScore statistics are grouped by period (`ALL`, `1ST`, `2ND`, etc.) and source group. The provider adapter maps the response into neutral snapshots; the match application service owns persistence. Re-sync replaces that provider's snapshot only when at least one valid statistic is returned, so an empty response never erases previously stored data.

## Manual SofaScore sync

Sync leagues, teams and matches first. Then fetch statistics for a known SofaScore event:

`POST /api/v1/ingestion/sofascore/matches/statistics/sync?eventId={sofaScoreEventId}`

Read canonical statistics:

`GET /api/v1/matches/{matchId}/statistics`

Ingestion requires the `X-Ingestion-Admin-Key` header. Statistics are retrieved from SofaScore's event endpoint `/api/v1/event/{eventId}/statistics`.

## Manual match sync

`POST /api/v1/ingestion/sofascore/matches/sync?tournamentId={sofaTournamentId}&sofaSeasonId={sofaSeasonId}&canonicalSeasonId={seasonId}&window=NEXT&page=0`

Use `window=NEXT` for upcoming events and `window=LAST` for completed/recent events. Pages are zero-based. The response reports created, updated and skipped event counts with warnings for teams that have not yet been mapped.

List canonical matches with:

`GET /api/v1/matches?seasonId={seasonId}`
