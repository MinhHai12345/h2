# ADR-001: Modular Monolith with Layered Architecture

- Status: Accepted
- Project: H2-Football
- Date: 2026-11-18

## Context

MMO contains proven football-data capabilities but has accumulated mixed module boundaries and legacy abstractions. H2-Football is a new application and database schema, so it can establish explicit ownership without copying the legacy package structure or schema wholesale.

## Decision

Use one Spring Boot deployable organized as a Modular Monolith. Each business module uses Layered Architecture with only the layers it needs:

- `api`: controllers and transport DTOs
- `application`: use cases, orchestration and transaction boundaries
- `domain`: business entities, value objects and invariants
- `repository`: persistence access owned by the module
- `infrastructure`: provider-specific or technical implementations

Do not mandate ports/adapters in every module. Use interfaces only for real external boundaries, multiple implementations or meaningful test seams.

## Module ownership

- `masterdata`: canonical leagues, seasons, teams, players and external identities.
- `match`: canonical fixtures, results, events, lineups and match observations.
- `provider`: provider clients and source-specific parsing/DTOs.
- `ingestion`: acquisition workflows, normalization, validation, idempotency and checkpoints.
- `workflow`: end-to-end execution stages, retries and coordination.
- `analytics`: statistical features, models, evaluation and backtesting.
- `content`: analysis-to-content transformation and editorial lifecycle.
- `publication`: delivery to external platforms.
- `operations`: diagnostics and operational visibility.

## Dependency rules

1. Controllers do not contain business/statistical logic and do not call repositories directly.
2. A module owns its entities and repositories.
3. Cross-module calls go through public application services or stable DTO contracts.
4. Provider DTOs never leak into canonical domain, analytics or public API contracts.
5. Provider modules do not persist canonical entities directly.
6. Analytics does not call provider clients.
7. Content does not recalculate analytics.
8. Publication does not generate content.

## Data and migration decision

- Use a new MySQL schema named `h2_football`.
- Use Flyway for all schema evolution.
- Keep Hibernate DDL generation disabled.
- Migrate MMO data in controlled slices, beginning with master data and external identifiers.
- Preserve source provenance and verify uniqueness, row counts and relationship integrity before cutover.
- Keep MMO operational until H2-Football passes data and workflow verification.

## Rejected alternatives

- Microservices: unnecessary deployment and distributed-consistency overhead at this stage.
- Full Hexagonal Architecture everywhere: disproportionate structural overhead.
- Big-bang code/schema copy: high migration and data-integrity risk.
