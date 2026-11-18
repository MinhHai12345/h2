# Match domain

The `match` module owns canonical fixtures, results and match-level team statistics. A match belongs to one canonical season and references distinct home and away teams. Kickoff timestamps are stored as UTC instants; scores remain nullable until the provider reports them.

Match status is normalized into the domain enum and does not expose provider-specific status strings. Provider event identifiers are mapped through the shared `external_identities` table using resource type `MATCH`; no match-specific identity entity is created.

The ingestion flow requires an existing canonical season and existing canonical team mappings. This prevents provider event payloads from creating incomplete master data implicitly. Register the SofaScore season identifier against the selected canonical season before importing events.

## Match statistics

Statistics are stored per match, provider, period and statistic key. The schema preserves provider display values and separately stores numeric values when they can be interpreted safely. Values such as `12/26 (46%)` remain intact and are not incorrectly converted into a single numeric measurement.

SofaScore statistics are grouped by period (`ALL`, `1ST`, `2ND`, etc.) and source group. The provider adapter maps the response into neutral snapshots; the match application service owns persistence. Re-sync replaces that provider's snapshot only when at least one valid statistic is returned, so an empty response never erases previously stored data.

## Lineups and incidents

Canonical players are owned by the `player` module. SofaScore player IDs are mapped through the shared identity table using resource type `PLAYER`. No `PlayerExternalIdentity` entity is introduced.

Lineups are stored per match, team and provider with formation and confirmation state. Each lineup player has a role of `STARTER`, `SUBSTITUTE` or `MISSING`, plus position, shirt number, captain flag and optional missing reason. A partial response only replaces the team lineups present in that response.

Incidents are stored in source order and preserve event type/class, minute, added time, side, running score, rescinded state and optional relationships to the main player, assist, player entering and player leaving. Period markers without a side are stored as `NEUTRAL`.

## Manual SofaScore sync

Sync leagues, teams and matches first. Then fetch statistics, lineups and incidents for a known SofaScore event:

- `POST /api/v1/ingestion/sofascore/matches/statistics/sync?eventId={sofaScoreEventId}`
- `POST /api/v1/ingestion/sofascore/matches/lineups/sync?eventId={sofaScoreEventId}`
- `POST /api/v1/ingestion/sofascore/matches/incidents/sync?eventId={sofaScoreEventId}`

Read canonical match data:

- `GET /api/v1/matches/{matchId}/statistics`
- `GET /api/v1/matches/{matchId}/lineups?provider=SOFASCORE`
- `GET /api/v1/matches/{matchId}/incidents?provider=SOFASCORE`

All ingestion endpoints require the `X-Ingestion-Admin-Key` header. Missing lineups (SofaScore HTTP 404) and empty incident responses do not erase previously stored data.
