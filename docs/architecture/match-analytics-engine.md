# Match analytics engine

## Scope and data integrity

The engine calculates a pre-match analytical snapshot for a canonical match and provider. Historical inputs are restricted to finished matches in the same season whose kickoff is strictly earlier than the target kickoff. This prevents future-match leakage when calculating a fixture.

For each team, the engine reads up to ten historical match-level `expectedGoals` observations in the `ALL` period and the latest five finished-match results. It does not use season aggregates because those may include matches played after the target fixture.

A result is persisted for each `(match_id, provider)`. Rebuilding updates the existing row and records the model version and computation timestamp.

## Model v1

### Expected goals

- Team attack baseline: mean of that team's historical expected goals for.
- Opponent defence baseline: mean of the opponent's historical expected goals against.
- Base expected goals: arithmetic mean of attack and opponent-defence baselines.
- Form adjustment: `lambda_adjusted = lambda_base * (1 + 0.15 * (team_form - opponent_form))`.

### EMA form

Match points are 3 for a win, 1 for a draw and 0 for a loss. The latest five results are weighted with EMA period 5 and alpha `2 / (5 + 1)`. The result is normalized to [0, 1] by dividing by three.

### Poisson outputs

Independent Poisson distributions are calculated for home and away expected goals. The score grid covers 0 through 10 goals for each team and is normalized over the represented grid.

The engine returns home-win, draw, away-win, over-2.5 and both-teams-to-score probabilities.

### Match Attractiveness Index

MAI is a transparent 0–100 descriptive index:

- Goal intensity = min(1, total expected goals / 4).
- Competitive balance = 1 - min(1, absolute expected-goals difference / 2.5).
- MAI = 100 * (0.65 * goal intensity + 0.35 * competitive balance).

MAI is a model feature, not a probability.

## Missing data

If any of the four required historical xG averages (home xG for, home xG against, away xG for, away xG against) is unavailable, status is `INSUFFICIENT_DATA`. Probability and MAI fields remain null. The response includes a reason and the available form/history counts.

## API

- `GET /api/v1/matches/{matchId}/analytics?provider=SOFASCORE`
- `GET /api/v1/seasons/{seasonId}/match-analytics?provider=SOFASCORE`
- `POST /api/v1/matches/{matchId}/analytics/rebuild?provider=SOFASCORE` (requires `X-Ingestion-Admin-Key`)

The rebuild endpoint is intentionally on-demand. Quartz orchestration can be added after this manual path is validated.
