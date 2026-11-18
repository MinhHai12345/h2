# Spring Data REST

Spring Data REST is installed as an additional HTTP adapter for repositories that are
intentionally designed for direct resource exposure.

## API boundary

- The Spring Data REST base path is `/api/data`.
- HAL JSON is the default representation.
- Repository detection is set to `ANNOTATED`: a repository is not exported unless it
  is explicitly annotated with `@RepositoryRestResource`.
- Existing module repositories remain internal by default. Do not annotate them merely
  to make CRUD endpoints appear.

## Layering and safety

The normal application API remains under `/api/v1` and must continue to call application
services. Spring Data REST is reserved for deliberately selected, low-risk resources or
read-oriented resources with an explicitly reviewed repository contract.

Before exporting a repository:

1. Confirm that direct repository access does not bypass required application workflows.
2. Review exposed associations, entity fields, paging, sorting, filtering, and write methods.
3. Prefer read-only exposure where possible; do not expose internal persistence entities
   or provider identity tables by default.
4. Add focused API tests for the exported resource and its allowed HTTP methods.

Do not expose a repository by changing the global detection strategy to `DEFAULT` or
`ALL`. The explicit annotation requirement is a security and module-boundary safeguard.
