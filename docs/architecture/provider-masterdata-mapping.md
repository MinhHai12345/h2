# Provider Master Data Mapping and Reconciliation

## Purpose

H2 keeps one canonical record for each League, Season, Team and Player while retaining
provider-native identifiers in `external_identities`. Provider IDs are never copied into
canonical primary keys.

## Explicit mapping API

Create one mapping:

```http
POST /api/v1/provider-master-data/mappings
Content-Type: application/json

{
  "provider": "SOFASCORE",
  "resourceType": "TEAM",
  "externalId": "42",
  "canonicalId": 100
}
```

Supported providers: `SOFASCORE`, `UNDERSTAT`, `FOOTBALL_DATA`, `FOOTBALL_DATA_UK`.

Supported resource types: `LEAGUE`, `SEASON`, `TEAM`, `PLAYER`.
The target canonical row must already exist. Repeating the same mapping is idempotent;
attempting to point the same provider resource to a different canonical ID returns a conflict.

Batch mappings are supported through `POST /api/v1/provider-master-data/mappings/batch`.
The batch is transactional: a conflicting mapping rolls back the entire batch.

## Reconciliation API

```http
GET /api/v1/provider-master-data/reconciliation
GET /api/v1/provider-master-data/reconciliation?resourceType=TEAM
```

The endpoint returns candidate groups with canonical IDs, labels, context and a
`REVIEW_REQUIRED` status. It does not merge, delete, or rewrite any row.

Candidate rules:
- League: same country and canonicalized league name.
- Season: same canonical league and normalized season code.
- Team: same country and canonicalized team name.
- Player: same normalized name and country code. Player candidates remain review-only
  because names and nationality alone are not a globally unique player identity.

## Automatic linking during provider ingestion

- League imports use country plus the canonical league-name key; ambiguous candidates fail closed.
- Team imports use country plus the shared team alias key; ambiguous candidates fail closed.
- Player ingestion links only through an existing provider external identity. Names and country
  codes are not sufficient proof of identity; unmatched provider players are created separately
  and surfaced as reconciliation candidates for explicit review.
- Season dates are required by the canonical Season model. Provider season IDs can be mapped
  to an already-created canonical season through the explicit mapping API; discovery metadata
  without dates is not used to fabricate a Season.

## Safe reconciliation policy

Candidate detection and entity merging are separate operations. Before any future merge workflow,
H2 must validate foreign-key dependencies and re-point dependent records (matches, lineups,
incidents, player statistics, season statistics and external identities) in one transaction.
No automatic merge is performed by this module.
