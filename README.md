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

Compilation and tests have intentionally not been run during intermediate implementation steps. Run final verification only after the implementation batch is complete.

## Java formatting

Java formatting is enforced by Spotless + google-java-format.

1. Install Maven and Java 17.
2. Enable the repository Git hook once in each local clone:

   ```bash
   git config core.hooksPath .githooks
   ```

3. Commit normally. The `.githooks/pre-commit` hook runs:

   ```bash
   mvn -q spotless:apply
   ```

   Any formatting changes to tracked Java files are staged automatically.

4. To format manually:

   ```bash
   mvn spotless:apply
   ```

5. To verify formatting without changing files:

   ```bash
   mvn spotless:check
   ```

The Git hook is installed once per clone; the workflow check runs in GitHub Actions for pull requests to `main` and pushes to `main`. Do not bypass the hook with `--no-verify`.

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

## Delivery phases

1. Foundation and architecture guardrails.
2. Master data and external identity mapping.
3. SofaScore provider and ingestion.
4. Match observations and statistics.
5. Workflow, scheduling and operations.
6. Analytics.
7. Content generation and publication.
8. Controlled migration from MMO.
