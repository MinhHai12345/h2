# Player Impact Analytics

## Purpose

Player Impact provides a pre-match, lineup-aware view of the expected contribution of the confirmed starting players. It is a descriptive analytical index, not a prediction of an individual match event.

## Data boundary

- A target match must have a confirmed provider lineup for each team.
- Only players with the STARTER lineup role contribute to the team starting-impact average.
- Historical observations are restricted to finished matches in the same season with kickoff strictly before the target match.
- Season-to-date aggregates are intentionally not used because they can contain observations recorded after the target kickoff.
- Substitute and missing-player records are not included in the starting-impact average.
- Missing historical data is represented as a null player score and reflected in team data coverage; it is never replaced with a fabricated score.

## Player score v1

The calculator uses the latest five available player match-statistic observations. Counting statistics are normalized per 90 minutes. The component indices are bounded to [0, 1] and combined into a 0–100 score.

- Attacking: goals, expected goals and total shots.
- Creation: assists, expected assists and key passes.
- Defensive: tackles, interceptions and clearances.
- Rating: average provider rating normalized against a documented 6.0–8.5 range.

The component weights vary by the player's lineup position group (forward, midfielder, defender, goalkeeper, or unknown). If a component has no source observations, remaining available components are reweighted rather than treating missing data as zero.

The v1 score is intentionally transparent and should be evaluated against subsequent team performance before it is incorporated into the Match Analytics expected-goals model.

## API

GET /api/v1/matches/{matchId}/player-impact?provider=SOFASCORE

The response contains both teams, lineup confirmation, starting-player count, player-level components, team average impact and data coverage.
