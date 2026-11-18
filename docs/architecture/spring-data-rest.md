# Spring Data REST

Spring Data REST exposes the canonical master-data resources as read-only HAL resources.
The regular application API remains responsible for all writes and business workflows.

## Configuration

- Base path: `/api/data`
- Default representation: HAL JSON
- Repository detection: `ANNOTATED`; only repositories explicitly annotated with
  `@RepositoryRestResource` are exported.
- Entity IDs are included in representations.
- The current exported resources are Country, League, Season and Team.

## Resources

| Entity | Collection endpoint | Item endpoint |
|---|---|---|
| Country | `GET /api/data/countries` | `GET /api/data/countries/{id}` |
| League | `GET /api/data/leagues` | `GET /api/data/leagues/{id}` |
| Season | `GET /api/data/seasons` | `GET /api/data/seasons/{id}` |
| Team | `GET /api/data/teams` | `GET /api/data/teams/{id}` |

Collection resources support Spring Data paging and sorting parameters such as
`page`, `size` and `sort`. Related entities are represented through HAL association
links; the repository fetch plans load the related Country or League when collections
and items are read.

## Write boundary

Spring Data REST mutation methods are explicitly hidden on each exported repository.
Create and update operations must continue through the validated `/api/v1` controllers
and application services. This prevents direct repository writes from bypassing
application rules and transaction orchestration.

## Adding another entity

1. Add a dedicated repository annotation with a stable, plural resource path.
2. Keep repository detection set to `ANNOTATED`.
3. Expose IDs for the entity in `DataRestConfiguration`.
4. Hide repository mutation methods unless a reviewed use case explicitly requires
   direct writes.
5. Add API tests for collection, item, associations and allowed HTTP methods.

Do not expose provider identity tables or internal persistence entities by default.
