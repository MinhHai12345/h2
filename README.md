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

1. Copy `.env.example` to `.env` and set local credentials.
2. Start MySQL: `docker compose up -d mysql`.
3. Run the application with the `dev` profile using your IDE or `./mvnw spring-boot:run`.

The project has intentionally not been compiled or tested as part of this scaffold delivery.

## Database safety

The application connects to the `h2_football` schema by default. Never point this application at the legacy MMO schema during normal development. Hibernate schema generation is disabled; Flyway owns schema changes. The initial scaffold deliberately creates no business tables before the master-data and match model is approved.

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
