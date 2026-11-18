# Player match statistics

Player match statistics belong to the `match` module because each observation describes one player's performance in one match. The canonical player record remains owned by the `player` module and is referenced through its canonical ID.

The model combines normalized columns for commonly queried measures (rating, minutes, goals, assists, shots, passing, defensive actions, xG and xA) with the complete source statistics JSON. This keeps analytics queries efficient without discarding provider-specific or newly introduced metrics.

A snapshot is unique by `match_id + player_id + provider`. Each player observation is upserted independently, so a partial lineup response does not erase statistics for players omitted from that response. If SofaScore has not published lineups or player statistics yet, the ingestion path leaves previously stored data untouched.

## SofaScore sync

Player statistics are currently read from the nested `statistics` object on each player in the event lineups response:

- `POST /api/v1/ingestion/sofascore/matches/player-statistics/sync?eventId={sofaScoreEventId}`

The match must already be mapped through the shared `MATCH` external identity. Player references are resolved through the shared `PLAYER` external identities and canonical Player application service.

Read stored data:

- `GET /api/v1/matches/{matchId}/player-statistics?provider=SOFASCORE`

The raw JSON is retained alongside normalized fields to support future metric expansion and provider mapping changes.
