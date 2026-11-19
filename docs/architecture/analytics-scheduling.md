# Analytics scheduling and operations

## Overview

Quartz coordinates recurring analytics use cases. Quartz Jobs are deliberately thin: they invoke the analytics scheduling application service and do not contain repository access, model logic or transaction orchestration.

Two independent jobs are registered when `app.analytics.scheduler.enabled=true`:

| Job | Default schedule (UTC) | Responsibility |
|---|---|---|
| Match analytics refresh | Every 30 minutes | Find NOT_STARTED fixtures in the next 72 hours and refresh stale analytics |
| Model evaluation | Daily at 02:00 | Evaluate stored prediction snapshots for seasons containing finished matches |

Cron expressions are configurable. Quartz currently uses a RAMJobStore, so schedules are recreated from configuration on application startup.

## Match analytics refresh

The refresh use case:

1. Loads fixtures with status `NOT_STARTED` whose kickoff is between the current UTC time and the configured lookahead horizon.
2. Iterates over configured providers (SofaScore by default).
3. Skips a match/provider pair if its current analytics were computed within the configured refresh window (six hours by default).
4. Rebuilds stale or missing analytics through `MatchAnalyticsApplicationService`.
5. Isolates failures per match/provider so one incomplete fixture does not stop the rest of the batch.

Only READY analytics computed before kickoff are recorded by the existing Model Evaluation service. INSUFFICIENT_DATA results are retried after the refresh window, allowing newly ingested statistics to become available.

## Model evaluation

The evaluation job discovers distinct seasons from prediction snapshots whose matches are FINISHED, grouped by configured provider. It delegates to `ModelEvaluationApplicationService`, which evaluates only snapshots with authoritative final scores and preserves their original probabilities.

Re-running the job is safe: unchanged results are not evaluated again. Corrected final scores are re-evaluated against the same original snapshot.

## Configuration

| Environment variable | Default | Description |
|---|---:|---|
| `ANALYTICS_SCHEDULER_ENABLED` | `true` | Enables Quartz and analytics jobs |
| `ANALYTICS_PROVIDERS` | `SOFASCORE` | Comma-separated provider enum values |
| `ANALYTICS_LOOKAHEAD_HOURS` | `72` | Upcoming fixture horizon |
| `ANALYTICS_REFRESH_AFTER_MINUTES` | `360` | Minimum age before recomputing a fixture |
| `ANALYTICS_MATCH_REFRESH_CRON` | `0 0/30 * * * ?` | Match refresh cron expression |
| `ANALYTICS_MODEL_EVALUATION_CRON` | `0 0 2 * * ?` | Evaluation cron expression |

The scheduler is single-instance only with the current memory-backed Quartz store. Do not scale the application horizontally with this configuration: each instance would own an independent schedule and could execute the same work. Before horizontal deployment, move Quartz to a shared JDBC JobStore and configure a stable scheduler instance identity.

## Operational guarantees and limitations

- `@DisallowConcurrentExecution` prevents overlapping executions of each job detail.
- Each match/provider and season/provider failure is logged and isolated.
- Existing application-service transaction boundaries remain responsible for persistence.
- The scheduler does not fetch missing provider data. Ingestion must populate match statistics and confirmed lineups before analytics can become READY.
- Jobs use UTC for the application clock and cron triggers.
- Misfired cron executions are skipped rather than replayed in a burst after downtime.
