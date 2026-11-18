# Module boundaries

```text
com.h2football
├── H2FootballApplication
├── bootstrap
│   └── configuration
├── shared
│   ├── exception
│   ├── response
│   ├── time
│   └── validation
├── infrastructure
│   ├── persistence
│   ├── http
│   ├── browser
│   ├── scheduling
│   └── observability
└── module
    ├── identity
    ├── masterdata
    ├── match
    ├── provider
    ├── ingestion
    ├── workflow
    ├── analytics
    ├── content
    ├── publication
    └── operations
```

Create subpackages only when real classes are introduced. Avoid empty package trees and generic catch-all packages such as `core`, `common`, `manager` or `utils`.

## Identity boundary

The identity module owns the shared mapping between provider resources and canonical H2 records. It is provider-agnostic and can map countries, leagues, seasons, teams, matches, players, coaches and venues without creating resource-specific mapping entities.

The unique source key is `resource_type + provider + external_id`. A canonical ID is interpreted within its resource type; it intentionally has no polymorphic foreign key because it may reference different canonical tables. Resource-owning application services validate that the canonical record exists before registering a mapping.

## Provider boundary

A provider client returns provider DTOs. Provider mappers convert them into neutral ingestion models. The ingestion application service validates and normalizes those models, then calls the owning `masterdata` or `match` application service to persist canonical records.

## SofaScore page loading

A page specification explicitly owns the page URL and its endpoint specifications. One browser page load may capture several endpoint responses. Endpoint matching, response status, parsing and fetch diagnostics remain in the SofaScore provider implementation. Canonical persistence remains outside the provider.

## Idempotency and provenance

- External identity is unique by provider + resource type + external ID.
- Reprocessing the same provider observation must not create duplicate canonical records.
- Persist source, source external ID, observation time and field provenance where multiple providers can contribute.
- Separate observed facts from calculated analytics values.
