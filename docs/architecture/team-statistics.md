# Team statistics

## Source of truth

Provider match statistics remain in `match_statistics`. Each row contains a provider, period, statistic key, and the home/away values. No duplicate team-match fact table is created. The team-match read service projects the correct side into a team/opponent view after verifying that the requested team participates in the match.

## Season aggregates

`team_season_statistics` stores rebuildable aggregates by season, team, provider, period and statistic key. Only finished matches are included. For each metric the rebuild stores:
- `matchesCount`: number of finished matches with a non-null value for that metric
- `totalValue`: sum of the metric across those matches
- `averageValue`: arithmetic mean across those matches

The model stays key-based to support provider statistic catalogues evolving over time. Aggregates are replaced atomically per season/provider when source rows exist. An empty source result does not erase previously built aggregates.

## API

- `GET /api/v1/teams/{teamId}/matches/{matchId}/statistics?provider=SOFASCORE`
- `GET /api/v1/seasons/{seasonId}/team-statistics?provider=SOFASCORE`
- `GET /api/v1/seasons/{seasonId}/teams/{teamId}/statistics?provider=SOFASCORE`
- `POST /api/v1/seasons/{seasonId}/team-statistics/rebuild?provider=SOFASCORE`

The rebuild endpoint requires `X-Ingestion-Admin-Key`. Source ingestion remains the existing SofaScore match-statistics sync operation.
