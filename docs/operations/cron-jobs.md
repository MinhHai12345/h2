# Cron Job Management

H2 follows the Cron Job lifecycle pattern used by MMO while keeping the implementation in the Operations module.

## Architecture

- `CronJob` is the application registry and stores job identity, allowed job class, cron expression, parameters, current state and last execution result.
- `CronJobApplicationService` coordinates the registry and Quartz lifecycle.
- `CronJobFactory` creates jobs through Spring, enabling constructor injection.
- `CronJobStateListener` records RUNNING, last execution time and SUCCESS/ERROR.
- `SofaScoreQuartzJobInitializer` reconciles configured season jobs at startup.
- Quartz uses the MySQL JDBC job store; Flyway owns both application and Quartz tables.

Quartz jobs are adapters only. Ingestion orchestration remains in `SofaScoreSeasonMatchIngestionService`.

## Job states

| State | Meaning |
|---|---|
| SCHEDULED | Job is registered and its trigger is active |
| RUNNING | A job execution is in progress |
| PAUSED | Future scheduled executions are paused |

Last execution outcome is tracked separately as SUCCESS or ERROR. A completed recurring job returns to SCHEDULED.

## Configuration

```yaml
spring:
  quartz:
    job-store-type: jdbc
    jdbc:
      initialize-schema: never
    properties:
      org:
        quartz:
          scheduler:
            instanceName: H2QuartzScheduler
            instanceId: AUTO
          jobStore:
            driverDelegateClass: org.quartz.impl.jdbcjobstore.StdJDBCDelegate
            tablePrefix: QRTZ_
            isClustered: false

app:
  ingestion:
    sofascore:
      job:
        enabled: true
        cron: "0 15 2 * * ?"
        seasons:
          - tournament-id: 17
            sofa-season-id: 52186
            canonical-season-id: 42
```

Set `SOFASCORE_INGESTION_JOB_ENABLED=true` and optionally `SOFASCORE_INGESTION_JOB_CRON`. Season identifiers are mandatory. Cron expressions use Quartz syntax and are interpreted in UTC.

The database must have Flyway migrations V12 and V13 applied before enabling Quartz JDBC. For multiple application instances, set `QUARTZ_CLUSTERED=true` only when all instances share the same database and scheduler configuration.

## Management API

All endpoints require `X-Ingestion-Admin-Key`, using the same `INGESTION_ADMIN_API_KEY` configured for ingestion operations.

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/operations/cron-jobs` | List registered jobs |
| GET | `/api/v1/operations/cron-jobs/{id}` | Get one job |
| POST | `/api/v1/operations/cron-jobs/{id}/start` | Trigger immediately |
| POST | `/api/v1/operations/cron-jobs/{id}/pause` | Pause future executions |
| POST | `/api/v1/operations/cron-jobs/{id}/resume` | Resume schedule |
| POST | `/api/v1/operations/cron-jobs/{id}/stop` | Interrupt current execution |
| PATCH | `/api/v1/operations/cron-jobs/{id}/schedule` | Update cron expression |
| DELETE | `/api/v1/operations/cron-jobs/{id}` | Delete registry entry and Quartz job |

Immediate execution can optionally override one or more season identifiers:

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
- This first implementation registers SofaScore season synchronization jobs. Other ingestion jobs can be added by registering approved job types and their application use cases.
