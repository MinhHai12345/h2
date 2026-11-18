# SofaScore master-data ingestion

## Scope

The first SofaScore ingestion slice supports:
- Discovering the regional football tournament catalogue.
- Capturing season responses emitted by the same football page navigation.
- Importing countries and leagues into canonical master data.
- Discovering teams from a selected tournament-season standings page.
- Importing teams and storing provider-to-canonical identities.

The provider module owns Playwright, page specifications, response capture, transport DTOs and mapping. It never writes canonical repositories. The ingestion application service coordinates provider reads and calls the public master-data import application service.

## Configuration

Set these values in `.env`:

| Variable | Default | Purpose |
|---|---|---|
| `SOFASCORE_ENABLED` | `false` | Enable the browser provider |
| `SOFASCORE_WEB_URL` | `https://www.sofascore.com` | SofaScore web origin |
| `SOFASCORE_REGION` | `VN` | Two-letter catalogue region |
| `SOFASCORE_HEADLESS` | `true` | Run browser without a visible window |
| `SOFASCORE_BROWSER_CHANNEL` | `chrome` | Installed Chromium channel |
| `SOFASCORE_PROFILE_DIRECTORY` | `.runtime/sofascore-profile` | Persistent browser profile |
| `INGESTION_ADMIN_API_KEY` | empty | Required to authorize ingestion endpoints |

Install the Playwright browser runtime on the host before enabling the provider. The configured browser channel must exist on that host. Keep the profile directory private; it may contain browser session data.

## Operations API

All operations require the `X-Ingestion-Admin-Key` header.

### Synchronize leagues

`POST /api/v1/ingestion/sofascore/leagues/sync`

Optional request body:

```json
{
  "tournamentIds": [17, 8]
}
```

Omit the body or send an empty list to use the complete regional catalogue. This operation imports country and league records and creates identities unique by resource type, provider and external ID.

### Synchronize teams

`POST /api/v1/ingestion/sofascore/teams/sync?tournamentId=17&seasonId=52186`

Teams are discovered from standings. If a competition does not expose standings, this first slice reports no teams; event-based fallback is a subsequent extension.

## Idempotency

Canonical records are reused by their natural key when possible:
- Country: ISO-2 code.
- League: deterministic provider-scoped code.
- Team: country + case-insensitive name.

A unique `masterdata_external_identities` row links the provider resource ID to its canonical ID. Re-running the same import reuses that mapping rather than creating another record.

## Season date integrity

SofaScore's tournament season catalogue supplies season ID, name/year and current status, but does not provide the actual competition start and end dates required by the canonical `Season` entity. The ingestion service therefore reports discovered seasons but deliberately does not fabricate dates or persist incomplete canonical seasons. A later match/event ingestion slice must establish reliable season boundaries before these seasons are imported.

## Current limitations

- This is an on-demand API workflow; Quartz scheduling is intentionally deferred until the manual ingestion path is verified.
- Team discovery currently uses standings only.
- Understat, football-data and football-data.uk providers are not enabled by this implementation.
- No compilation or tests are claimed until the final verification stage.
