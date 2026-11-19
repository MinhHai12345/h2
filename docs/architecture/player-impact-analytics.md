# Player Impact Analytics

## Purpose

Player Impact provides a pre-match, lineup-aware view of the expected contribution of the confirmed starting players. Its role-aware indices are descriptive model features, not predictions of individual match events.

## Data boundary

- A target match must have a confirmed provider lineup for each team before lineup impact can adjust Match Analytics.
- Only players with the STARTER lineup role contribute to team component averages.
- Historical observations are restricted to finished matches in the same season with kickoff strictly before the target match.
- Season-to-date aggregates are intentionally not used because they can contain observations recorded after the target kickoff.
- Substitute and missing-player records are not included in starting-player component averages.
- Missing historical data is represented as a null player component and reflected in team data coverage; it is never replaced with a fabricated score.

## Player score v1

The calculator uses the latest five available player match-statistic observations. Counting statistics are normalized per 90 minutes. Component indices are bounded to [0, 1] and combined into a 0–100 score.

- Attacking: goals, expected goals and total shots.
- Creation: assists, expected assists and key passes.
- Defensive: tackles, interceptions and clearances.
- Rating: average provider rating normalized against a documented 6.0–8.5 range.

The component weights vary by the player's lineup position group (forward, midfielder, defender, goalkeeper, or unknown). If a component has no source observations, remaining available components are reweighted rather than treating missing data as zero.

Team impact responses expose average attacking and defensive component indices in addition to the overall starting-impact index and historical data coverage. Match Analytics v2 consumes only the attacking and defensive component indices.

## Integration constraints

- Only confirmed starters are included.
- Missing attacking or defensive components remain null.
- Both teams' lineups must be confirmed for any adjustment.
- The lower of the two lineup coverage percentages scales the adjustment.
- The maximum adjustment to either team's expected goals is ±5%.
- If lineup data is incomplete, Match Analytics preserves its team xG + EMA baseline.

## API

GET /api/v1/matches/{matchId}/player-impact?provider=SOFASCORE

The response contains both teams, lineup confirmation, starting-player count, player-level components, average team attacking/defensive indices, average overall impact and data coverage.
