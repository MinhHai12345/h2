# H2 Canonical Football Data Standard

## Purpose

H2 stores one provider-neutral representation of football data. Provider payloads and provider IDs
belong in provider DTOs and the shared external-identity registry; they must not define canonical
entity names or database keys.

The ingestion boundary is:
`Provider API Client -> Provider DTO -> Provider Mapper/Standardizer -> Application Service -> Canonical Repository`

Provider clients own HTTP and source response DTOs. Mappers translate source-specific fields into
provider-neutral records. Application services resolve identities and persist canonical entities.
Repositories never receive provider DTOs.

## Canonical vocabulary

| H2 concept | Canonical name | Meaning |
|---|---|---|
| Country | `Country.isoCode`, `Country.name` | Country/territory identity |
| League | `League.code`, `League.name` | Competition independent of provider |
| Season | `Season.seasonCode`, `displayName`, `startDate`, `endDate` | One league edition and its actual date range |
| Team | `Team.name` | Canonical club/national-team name |
| Match | `Match.kickoffAt`, `status`, `homeScore`, `awayScore`, `roundName` | Canonical fixture |
| Player | `Player` | Person registered as a football player |
| Match statistic | `MatchStatistic.statisticKey`, `period`, `homeValue`, `awayValue` | Source-attributed match metric |
| Lineup | `MatchLineup`, `MatchLineupPlayer` | Team selection and player role for a match |
| Incident | `MatchIncident` | Timestamped match event |
| Player match statistic | `MatchPlayerStatistic` | Player performance metric for one match |
| Team season statistic | `TeamSeasonStatistic` | Aggregated team metric in a season |
| Player season statistic | `PlayerSeasonStatistic` | Aggregated player metric in a season |
| External identity | `resourceType + provider + externalId -> canonicalId` | Stable link from a source ID to an H2 entity |

## Naming conventions

- Java entities and fields use singular domain nouns and camelCase: `League`, `Season`,
  `kickoffAt`, `expectedGoals`.
- Database tables use plural snake_case: `leagues`, `seasons`, `matches`.
- Canonical metric keys use stable lower-camelCase; display labels are separate. Examples:
  `expectedGoals`, `totalShots`, `shotsOnTarget`, `bigChances`, `possession`,
  `passAccuracy`, `corners`, `fouls`, `yellowCards`, `redCards`.
- Time instants are UTC `Instant`; competition calendar dates are `LocalDate`; durations use
  explicit units in field names.
- Scores and counts are non-negative integers. Rates, percentages, xG and model values use
  `BigDecimal`; percentages use 0-100 and xG is not a percentage.
- A season is not just a year: `seasonCode` is a stable machine key (for example `2025-2026`),
  `displayName` is presentation text (for example `2025/2026`), and date bounds are explicit.
- Home/away values remain directional. Do not collapse them into an unordered pair.

## Provider identity and provenance

- Never normalize, rewrite, or infer a provider external ID. Store the exact source identifier
  (trimmed only) under `ExternalIdentity`.
- External identity uniqueness is scoped by resource type and provider.
- A canonical entity can have many provider identities. An identity cannot silently point to two
  canonical entities; conflicts must fail and be reviewed.
- Match statistics retain provider attribution. The canonical statistic key is shared, while
  provider, source display values and original source provenance remain source-specific.
- Missing data is null/unknown, not zero. Do not turn absent statistics into zero values.
- Never overwrite a known finished score with a null or lower-confidence secondary-source value.

## Standardization policy

`ProviderDataStandardizer` is the shared entry point for canonical league/team names.
Known aliases are explicit and provider-neutral. Unknown names are preserved after trimming and
whitespace compaction. Comparison keys fold accents and punctuation but deliberately retain generic
club words to avoid false-positive team merges. Name similarity alone is not sufficient to link
entities: provider external IDs take precedence, and ambiguous matches must be rejected.

League/season mappings must be explicit where a provider competition code differs. SofaScore
tournament IDs, Understat league slugs, football-data.org competition codes and Football-Data.co.uk
league codes are not interchangeable with H2 league IDs.

## Provider capability boundary

SofaScore is the broad match-detail source in the current H2 integration. Understat contributes
historical fixtures/results and expected goals for supported competitions. football-data.org
contributes competition fixtures/results and supported standings/team metadata. Football-Data.co.uk
contributes historical fixture/results and CSV statistics where present. A provider not exposing a
field must not cause H2 to synthesize it. Provider coverage is tracked independently from canonical
schema completeness.

## Migration policy

Canonical naming changes must not rewrite provider IDs or canonical primary keys. Database column
renames require a Flyway migration and API compatibility review. Prefer additive fields and explicit
mapping tables over destructive renames. Existing `seasonCode` and `displayName` semantics remain
unchanged.
