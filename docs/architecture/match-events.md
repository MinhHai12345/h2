# Match lineups and incidents

Player records are canonical resources owned by the `player` module. SofaScore player IDs are mapped through the shared `external_identities` table with resource type `PLAYER`; no provider-specific player identity table is used.

A match lineup is stored per match, participating team and provider. It contains the source formation and confirmation state. Player rows distinguish starters, substitutes and missing players, and preserve position, shirt number, captain flag and missing reason.

Match incidents preserve the source event identifier, source ordering, incident type/class, minute and added time, home/away side, running score, rescinded state and optional player relationships (scorer, assist, player entering and player leaving). Neutral incidents such as period markers do not invent a team side.

## Manual SofaScore sync

Both endpoints require a canonical match already mapped to the SofaScore event through the shared `MATCH` external identity.

- `POST /api/v1/ingestion/sofascore/matches/lineups/sync?eventId={sofaScoreEventId}`
- `POST /api/v1/ingestion/sofascore/matches/incidents/sync?eventId={sofaScoreEventId}`

Read stored data:

- `GET /api/v1/matches/{matchId}/lineups?provider=SOFASCORE`
- `GET /api/v1/matches/{matchId}/incidents?provider=SOFASCORE`

A missing lineup (SofaScore HTTP 404) is treated as not-yet-published data and does not erase the previous snapshot. An empty incident list likewise leaves the previously stored snapshot unchanged. Non-empty snapshots replace only data from the same provider.

The integration uses SofaScore's public event endpoints `/api/v1/event/{eventId}/lineups` and `/api/v1/event/{eventId}/incidents`.
