# Player season statistics

Player season statistics are derived from the canonical match-level player observations stored in `match_player_statistics`. They are not fetched as a separate source dataset. This keeps season totals consistent with the match records used by the analytics engine and makes the aggregation reproducible.

The analytics application service groups match observations by player for one canonical season and provider. The result is stored in `player_season_statistics`, uniquely keyed by `season_id + player_id + provider`.

## Aggregated measures

- Appearances: number of match-player statistic records.
- Minutes, goals, assists, shots, shots on target, accurate passes, total passes, key passes, tackles, interceptions and clearances: sums across matches.
- Expected goals and expected assists: sums across matches.
- Average rating: arithmetic mean of non-null match ratings.

The source match observations remain the source of truth. Rebuilding a season updates existing aggregates and inserts new player aggregates. An empty source result does not delete previously stored season rows.

## API

Read aggregates:

- `GET /api/v1/seasons/{seasonId}/player-statistics?provider=SOFASCORE`

Rebuild aggregates:

- `POST /api/v1/seasons/{seasonId}/player-statistics/rebuild?provider=SOFASCORE`
- Header: `X-Ingestion-Admin-Key`

The rebuild operation is protected by the same configured operations key as ingestion endpoints.
