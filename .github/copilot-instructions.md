# GitHub Copilot Instructions — H2 Football

## Project context
- This repository is H2 Football, a Java 17 / Spring Boot 3 modular monolith for football data ingestion, analytics, content generation, and publication.
- Use the existing repository structure and conventions as the source of truth. Inspect related modules and tests before proposing or changing code.
- Keep the architecture modular and layered. Do not introduce microservices or force Hexagonal Architecture without an explicit architectural decision.

## Architecture rules
- Keep responsibilities separated: Controller / Scheduler -> Application Service -> Domain logic and Repository.
- Controllers handle HTTP concerns only. Schedulers trigger application workflows only. Do not place business or statistical logic in controllers, schedulers, scrapers, or provider adapters.
- Modules communicate through public application services and explicit contracts. Do not access another module's repositories or JPA entities directly.
- Keep provider-specific DTOs, payloads, parsing, and identifiers inside the relevant provider integration. Map provider data into canonical application/domain models before persistence.
- Provider adapters fetch and parse source data; ingestion coordinates normalization, validation, idempotency, and persistence.
- Analytics consumes persisted canonical data and must not call external providers.
- Content generation consumes persisted analytics; it must not recalculate analytics.
- Publication delivers prepared content; it must not fetch match data or generate analytics/content.
- Keep shared code limited to genuinely cross-cutting concerns. Do not create a catch-all `core` package.

## Data and persistence
- Use MySQL with schema `h2_football` in local/deployment configuration unless explicitly changed.
- Use Flyway for schema evolution. Do not rely on Hibernate schema auto-generation in production; use `ddl-auto: validate` or `none` as appropriate.
- Preserve source provenance and external identity mappings for imported data.
- Design ingestion to be idempotent: repeated processing of the same provider record must not create duplicate canonical records.
- Store canonical league, season, team, player, and match identities separately from provider-specific identifiers.
- Use explicit UTC/time-zone handling and `Clock` injection for time-dependent logic.

## Code quality
- Prefer clear, idiomatic Java 17, SOLID design, constructor injection, immutable DTOs where practical, and small cohesive classes.
- Use Bean Validation at input boundaries and centralized exception handling.
- Add structured, useful logs. Never log credentials, tokens, or sensitive configuration.
- Add JUnit 5 / Mockito tests for important service logic, mappers, parsers, and idempotency behavior.
- Keep tests deterministic; inject `Clock` and avoid real network/browser/database dependencies in unit tests.
- Handle WebClient and Playwright lifecycle, timeouts, retries, and resource cleanup explicitly.
- Avoid speculative abstractions and unnecessary dependencies. Follow existing dependency versions and package conventions.

## Terminology restriction
- Never use betting, gambling, wagering, odds, or related terminology in source code, identifiers, comments, logs, API contracts, documentation, or generated content.
- Use neutral data-science terminology such as expected goals (xG), probability distribution, statistical indicators, match analysis, and model evaluation.
- Do not introduce fields or concepts that encode betting-market data.

## Change workflow
1. Inspect the current repository structure and relevant implementation before editing.
2. Explain the impact on module boundaries and persistence before broad changes.
3. Make the smallest complete change consistent with the architecture.
4. Update or add focused tests and documentation with the implementation.
5. Do not claim compilation or tests passed unless they were actually executed and their results are known.
6. Do not run compile, test, migration, or destructive database commands unless the user explicitly asks.
7. Do not silently rewrite unrelated modules, schema, or configuration.

## Dependency direction
- `bootstrap` and `infrastructure` may wire implementations together.
- Feature modules should depend on their own domain/application layers and declared contracts, not on infrastructure implementation details.
- Domain logic should not depend on Spring MVC, Playwright, WebClient, JPA, or provider SDKs.
