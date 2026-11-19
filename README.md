# H2-Football

Football data ingestion, analytics and social publishing platform.

## Architecture

- Java 17
- Spring Boot 3.4.x
- Modular Monolith
- Layered Architecture inside business modules
- MySQL with Flyway-managed schema
- Quartz for scheduled triggers
- WebClient for HTTP integrations
- Playwright for browser-based providers such as SofaScore

MMO remains the legacy/reference application. H2-Football uses a new repository and a new database schema. Existing MMO code and data are migrated selectively after contract and data-quality review.

## Local setup

1. Copy `.env.example` to `.env` and set local database credentials.
2. Start MySQL: `docker compose up -d mysql`.
3. Run the application with the `dev` profile using your IDE or `mvn spring-boot:run`.
4. Keep all providers disabled until their browser/API prerequisites and credentials are configured.

For the first SofaScore ingestion flow, configure `SOFASCORE_ENABLED=true`, install the configured Playwright browser channel, set a private browser profile directory and configure a strong `INGESTION_ADMIN_API_KEY`. See [SofaScore master-data ingestion](docs/ingestion/sofascore-masterdata.md) for endpoint usage and current limitations.

## Java formatting

Java formatting is enforced by Spotless + google-java-format.

1. Install Maven and Java 17.
2. Enable the repository Git hook once in each local clone:

   ```bash
   git config core.hooksPath .githooks
   ```

3. Commit normally. The `.githooks/pre-commit` hook runs `mvn -q spotless:apply` and stages changed tracked Java files.
4. Run `mvn spotless:apply` to format manually or `mvn spotless:check` to verify without changing files.

The workflow check runs in GitHub Actions for pull requests to `main` and pushes to `main`. Do not bypass the hook with `--no-verify`.

## Database safety

The application connects to the `h2_football` schema by default. Never point this application at the legacy MMO schema during normal development. Hibernate schema generation is disabled; Flyway owns schema changes.

## Modules

| Module | Responsibility |
|---|---|
| `masterdata` | Canonical leagues, seasons, teams, players and external identities |
| `match` | Fixtures, results, lineups, events and match observations |
| `provider` | Provider-specific clients, DTOs and parsing |
| `ingestion` | Fetch, normalize, validate and persist source data |
| `workflow` | Cross-module execution coordination |
| `analytics` | Statistical features, models, evaluation and backtesting |
| `content` | Editorial artifacts and LLM-assisted generation |
| `publication` | Telegram, Facebook and other delivery channels |
| `operations` | Operational APIs, diagnostics and execution visibility |

## Dependency rules

- API controllers call application services, never repositories directly.
- Application services own use-case orchestration and transaction boundaries.
- Domain code does not depend on Spring MVC, provider DTOs or external HTTP clients.
- A module accesses another module through its public application service/contract, not its repositories or JPA entities.
- Provider integrations never write directly to canonical JPA repositories.
- Analytics consumes persisted canonical data; it never scrapes providers.
- Content consumes persisted analytics outputs; publication delivers approved content.
- Do not create interfaces or package layers without a concrete need.

## Analytics APIs

- `GET /api/v1/matches/{matchId}/analytics?provider=SOFASCORE`
- `GET /api/v1/matches/{matchId}/player-impact?provider=SOFASCORE`
- `GET /api/v1/seasons/{seasonId}/match-analytics?provider=SOFASCORE`
- `GET /api/v1/seasons/{seasonId}/player-performance?provider=SOFASCORE`
- `GET /api/v1/seasons/{seasonId}/model-evaluation?provider=SOFASCORE`
- `POST /api/v1/seasons/{seasonId}/model-evaluation/rebuild?provider=SOFASCORE` (requires `X-Ingestion-Admin-Key`)

Player Impact requires confirmed lineups for the target match. It uses only player match-statistic observations from finished matches in the same season before the target kickoff. Its 0–100 component-weighted index is descriptive; missing historical data remains null and is reported through team data coverage.

Match Analytics v2 integrates confirmed-lineup attacking and defensive components into the Poisson + EMA expected-goals baseline. The adjustment is coverage-weighted and bounded to ±5%; missing or unconfirmed lineups preserve the baseline. Prediction snapshots are stored under the explicit model version `poisson-ema-player-impact-v2`, separate from earlier model versions.

Model Evaluation captures immutable pre-match probability snapshots and evaluates them against completed results. Reports are separated by model version and include multiclass Brier Score, Log Loss and ten-bin confidence calibration. See [Model Evaluation](docs/architecture/model-evaluation.md) and [Match Analytics](docs/architecture/match-analytics-engine.md).

## Analytics scheduling

Quartz refreshes stale analytics for `NOT_STARTED` fixtures in the next 72 hours every 30 minutes and evaluates snapshots for finished matches daily at 02:00 UTC. Refreshes are rate-limited per match/provider to once every six hours by default. Configure providers, horizons and cron expressions through `ANALYTICS_*` environment variables. The current RAMJobStore is single-instance only; use a shared JDBC JobStore before horizontal scaling. See [Analytics Scheduling](docs/architecture/analytics-scheduling.md).

## Delivery phases

1. Foundation and architecture guardrails.
2. Master data and external identity mapping.
3. SofaScore provider and ingestion.
4. Match observations and statistics.
5. Workflow, scheduling and operations.
6. Analytics.
7. Content generation and publication.
8. Controlled migration from MMO.
