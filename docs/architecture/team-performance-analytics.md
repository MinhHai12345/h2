# Team performance analytics

## Data sources

Team performance is a read model over existing canonical data:
- matches supplies finished-match scores and home/away outcomes.
- team_season_statistics supplies provider-specific per-match averages derived from match statistics.
- match_statistics supplies opponent expected-goals observations for expected goals against.

No duplicate team-performance table is introduced. Missing provider metrics remain null; the calculator does not infer values from display strings or substitute zero for unavailable data.

## Metrics

- Goals for / against: totals and per-match averages from finished matches with both scores present.
- Expected goals for: average expectedGoals statistic from the team's perspective.
- Expected goals against: average opponent-side expectedGoals from the same finished matches.
- Expected goals difference: expected goals for minus expected goals against.
- Shots, shots on target and possession: per-match averages from ALL period season aggregates.
- Shot accuracy: shots on target divided by total shots, expressed as a percentage.
- Home/away splits: match count, goals for/against and per-match averages.

Provider statistic keys are treated as source identifiers. Current SofaScore keys include expectedGoals and ballPossession; aliases are supported for common shot-key naming variants. Ratio displays such as 12/26 (46%) remain non-numeric and are not used as numeric inputs.

## API

- GET /api/v1/seasons/{seasonId}/team-performance?provider=SOFASCORE
- GET /api/v1/seasons/{seasonId}/team-performance/{teamId}?provider=SOFASCORE

The response combines season-level performance with home/away goal splits. Rebuild the underlying team season statistics first using the existing protected rebuild endpoint.
