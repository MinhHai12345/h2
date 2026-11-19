# Cron Job Management

H2 follows the Cron Job lifecycle pattern used by MMO while keeping the implementation in the Operations module.

## Architecture

- `CronJob` is the application registry and stores job identity, approved job class, cron expression, parameters, current state and last execution result.
- `CronJobApplicationService` coordinates the registry and Quartz lifecycle.
- `CronJobFactory` creates jobs through Spring, enabling constructor injection.
- `CronJobStateListener` records RUNNING, last execution time and SUCCESS/ERROR.
- `SofaScoreQuartzJobInitializer` reconciles all enabled SofaScore job definitions at startup.
- Quartz uses the MySQL JDBC job store; Flyway owns both application and Quartz tables.

Quartz jobs are thin adapters. Ingestion orchestration remains in application services.

## Registered ingestion jobs

| Job group | Job | Responsibility |
|---|---|---|
| `sofascore-masterdata` | `SofaScoreLeagueCatalogueSyncJob` | Import tournament catalogue and discover provider seasons |
| `sofascore-ingestion` | `SofaScoreSeasonMatchSyncJob` | Synchronize teams and traverse all match event windows for a configured season |

The season synchronization use case imports prerequisite teams before traversing match pages. Match details are persisted through the existing match ingestion service.

## Job states

| State | Meaning |
|---|---|
| SCHEDULED | Job is registered and its trigger is active |
| RUNNING | A job execution is in progress |
| PAUSED | Future scheduled executions are paused |

Last execution outcome is tracked separately as SUCCESS or ERROR. A completed recurring job returns to SCHEDULED.

## Initial configuration

The following is the complete initial configuration for the currently implemented SofaScore jobs:

```yaml
app:
  ingestion:
    sofascore:
      job:
        enabled: true
        cron: "0 15 2 * * ?"
        run-on-startup: false
        seasons:
          - tournament-id: 17
            sofa-season-id: 52186
            canonical-season-id: 42
        league-catalogue:
          enabled: true
          cron: "0 0 2 * * ?"
          run-on-startup: true
          # Empty means fetch the complete available catalogue.
          tournament-ids: []
```

Environment variables:

| Variable | Default | Purpose |
|---|---|---|
| `SOFASCORE_INGESTION_JOB_ENABLED` | `false` | Enable recurring season jobs |
| `SOFASCORE_INGESTION_JOB_CRON` | `0 15 2 * * ?` | Season synchronization schedule |
| `SOFASCORE_INGESTION_JOB_RUN_ON_STARTUP` | `false` | Trigger each configured season once after startup |
| `SOFASCORE_LEAGUE_CATALOGUE_JOB_ENABLED` | `false` | Enable tournament catalogue job |
| `SOFASCORE_LEAGUE_CATALOGUE_JOB_CRON` | `0 0 2 * * ?` | Catalogue synchronization schedule |
| `SOFASCORE_LEAGUE_CATALOGUE_JOB_RUN_ON_STARTUP` | `false` | Trigger catalogue import once after startup |

Tournament identifiers are optional for catalogue synchronization. An empty list requests the complete available catalogue. Season identifiers are mandatory and must be positive. Cron expressions use Quartz syntax and are interpreted in UTC.

`run-on-startup` is an explicit operational switch. When enabled, the job is triggered on every application startup; keep it disabled after the required initial synchronization if repeated startup imports are not desired. The provider and ingestion layers remain idempotent so repeated imports update existing records rather than creating duplicates.

Disabling a job group pauses its existing definitions instead of deleting operational history. Season definitions removed from configuration are deleted from the registry and Quartz schedule.

The database must have Flyway migrations V12 and V13 applied before enabling Quartz JDBC. For multiple application instances, set `QUARTZ_CLUSTERED=true` only when all instances share the same database and scheduler configuration.

## Management API

All endpoints require `X-Ingestion-Admin-Key`, using the same `INGESTION_ADMIN_API_KEY` configured for ingestion operations.

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/operations/cron-jobs/metadata` | Read Quartz scheduler metadata |
| GET | `/api/v1/operations/cron-jobs` | List registered jobs |
| GET | `/api/v1/operations/cron-jobs/{id}` | Get one job |
| POST | `/api/v1/operations/cron-jobs/{id}/start` | Trigger immediately |
| POST | `/api/v1/operations/cron-jobs/{id}/pause` | Pause future executions |
| POST | `/api/v1/operations/cron-jobs/{id}/resume` | Resume schedule |
| POST | `/api/v1/operations/cron-jobs/{id}/stop` | Interrupt current execution |
| PATCH | `/api/v1/operations/cron-jobs/{id}/schedule` | Update cron expression |
| DELETE | `/api/v1/operations/cron-jobs/{id}` | Delete registry entry and Quartz job |

Immediate season execution can optionally override one or more season identifiers:

```json
{
  "tournamentId": 17,
  "sofaSeasonId": 52186,
  "canonicalSeasonId": 42
}
```

Only positive numeric values for those three supported parameters are accepted. A paused job must be resumed before immediate execution.

## Operational guarantees and limitations

- Job definitions and Quartz triggers are persisted in MySQL.
- `@DisallowConcurrentExecution` prevents overlapping executions for the same Quartz JobKey.
- Stop interrupts the job thread; season traversal checks interruption between event pages. An in-flight provider/browser request may take time to return before interruption is observed.
- Only registered job classes are accepted. The API does not accept arbitrary Java class names.
- Other provider jobs should be added only when their ingestion application use case and required parameters are implemented.


## Match detail orchestration

After teams and season match pages have been synchronized, the season ingestion use case processes each unique, successfully mapped event in this order:

1. Match statistics
2. Lineups
3. Incidents
4. Player match statistics

Player match statistics are sourced from the lineup response, so that step runs only after lineup ingestion succeeds. Statistics and incidents are independent of lineup ingestion and continue even if another stream fails.

Each stream has its own bounded retry loop. A failed stream is retried without restarting successful streams for the same event. Other independent streams and other events continue to process. If any stream remains unsuccessful after its configured attempts, the season Quartz execution is marked as failed; the next scheduled/manual execution safely replays the idempotent ingestion operations.

Configuration:

```yaml
app:
  ingestion:
    sofascore:
      job:
        match-details:
          enabled: true
          retry-attempts: 3
          retry-delay-millis: 1000
```

Environment variables:
- `SOFASCORE_MATCH_DETAILS_ENABLED`: enable match detail orchestration after season match synchronization.
- `SOFASCORE_MATCH_DETAILS_RETRY_ATTEMPTS`: maximum attempts per stream and event (minimum effective value is 1).
- `SOFASCORE_MATCH_DETAILS_RETRY_DELAY_MILLIS`: base delay between attempts; each subsequent retry waits a multiple of this value.

When match details are disabled, the season job still imports teams and matches but does not call the four detail ingestion services.
