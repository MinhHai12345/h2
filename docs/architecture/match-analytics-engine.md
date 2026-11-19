# Match analytics engine

## Scope and data integrity

The engine calculates a pre-match analytical snapshot for a canonical match and provider. Historical team inputs are restricted to finished matches in the same season whose kickoff is strictly earlier than the target kickoff. Player inputs are restricted to the target match's confirmed starting lineups and finished player match-statistic observations in the same season before kickoff. These boundaries prevent future-match leakage.

For each team, the baseline reads up to ten historical match-level `expectedGoals` observations in the `ALL` period and the latest five finished-match results. It does not use season aggregates because those may include matches played after the target fixture.

A result is persisted for each `(match_id, provider)`. Rebuilding updates the current read model and records its model version and computation timestamp. Prediction evaluation snapshots retain the model version and original probabilities independently.

## Model v1 baseline

### Expected goals

- Team attack baseline: mean of that team's historical expected goals for.
- Opponent defence baseline: mean of the opponent's historical expected goals against.
- Base expected goals: arithmetic mean of attack and opponent-defence baselines.
- Form adjustment: `lambda_adjusted = lambda_base * (1 + 0.15 * (team_form - opponent_form))`.

### EMA form

Match points are 3 for a win, 1 for a draw and 0 for a loss. The latest five results are weighted with EMA period 5 and alpha `2 / (5 + 1)`. The result is normalized to [0, 1] by dividing by three.

## Model v2: confirmed-lineup impact

Model v2 adds Player Impact after the team xG and EMA adjustments and before the Poisson calculation.

- The attacking component is the mean attacking index of confirmed starters for the team creating the expected-goals estimate.
- The defensive component is the mean defensive index of confirmed starters for the opposing team.
- Both components are bounded to [0, 1]. Missing components remain null and do not become zero.
- The coverage factor is the lower data-coverage percentage of the two confirmed starting lineups. If either lineup is unconfirmed or missing, coverage is zero and the baseline is unchanged.
- The adjustment is `lambda_v2 = lambda_v1 * (1 + 0.05 * (team_attack_index - opponent_defence_index) * combined_coverage)`, where coverage is expressed as a fraction from 0 to 1.
- Therefore lineup impact can change expected goals by at most ±5%. It cannot dominate the historical team baseline.
- Home and away expected goals use their own attacking component and the opponent's defensive component.

The model version is `poisson-ema-player-impact-v2`. The previous `poisson-ema-v1` snapshots remain separately evaluable; results from different model versions must not be combined.

## Poisson outputs

Independent Poisson distributions are calculated for home and away expected goals. The score grid covers 0 through 10 goals for each team and is normalized over the represented grid.

The engine returns home-win, draw, away-win, over-2.5 and both-teams-to-score probabilities.

## Match Attractiveness Index

MAI is a transparent 0–100 descriptive index:

- Goal intensity = min(1, total expected goals / 4).
- Competitive balance = 1 - min(1, absolute expected-goals difference / 2.5).
- MAI = 100 * (0.65 * goal intensity + 0.35 * competitive balance).

MAI is recalculated from the final adjusted expected-goals values. It is a model feature, not a probability.

## Missing data

If any of the four required historical xG averages (home xG for, home xG against, away xG for, away xG against) is unavailable, status is `INSUFFICIENT_DATA`. Probability and MAI fields remain null.

Player Impact is optional. If a confirmed lineup or historical player component is unavailable, its component remains null and the adjustment falls back to the unmodified team xG + EMA baseline. Team-level data coverage remains available through the Player Impact API.

## API

- `GET /api/v1/matches/{matchId}/analytics?provider=SOFASCORE`
- `GET /api/v1/seasons/{seasonId}/match-analytics?provider=SOFASCORE`
- `POST /api/v1/matches/{matchId}/analytics/rebuild?provider=SOFASCORE` (requires `X-Ingestion-Admin-Key`)

The rebuild endpoint is intentionally on-demand. Quartz orchestration can be added after this manual path is validated.
