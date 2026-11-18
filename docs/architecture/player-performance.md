# Player performance analytics

Player performance is calculated from canonical player season-statistic snapshots. Match-level player observations remain the source of truth; season statistics are rebuildable aggregates and performance values are derived at read time.

## Derived metrics

- Per 90 minutes: goals, assists, goal contributions, xG, xA, shots, key passes and defensive actions (tackles + interceptions + clearances).
- Rates: shots on target / total shots, accurate passes / total passes and goals / total shots.
- xG overperformance: goals minus expected goals.

Rates are rounded to three decimal places using HALF_UP. Metrics with missing source values, zero minutes or a zero denominator are returned as null, not zero. This distinguishes unavailable metrics from measured zero output.

## API

GET /api/v1/seasons/{seasonId}/player-performance?provider=SOFASCORE&minimumMinutes=450&position=F

- provider defaults to SOFASCORE.
- minimumMinutes defaults to 0.
- position is optional and matched case-insensitively against the canonical player position.
- Results are ordered by goal contributions per 90 minutes, then average rating, descending.

No additional database table is introduced: performance values are deterministic calculations over existing season aggregates and can be recalculated whenever underlying observations change.
