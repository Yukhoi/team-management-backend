# FLA HTML client migration

The existing `flaRestClient` now calls
`GET https://football-loisir-amateur.fr/championships/{championshipId}/rankings?season={seasonId}`
with `Accept: text/html`. Jsoup parses the response into the unchanged
`FlaStandingEntryResponse` contract. Sync, cache writes, mappings, snapshots,
metrics, AI generation, database schema and public API contracts are unchanged.

## Parsing and compatibility

The parser identifies a unique table by the Équipe and Points headings and
requires all twelve expected columns. It maps fields by heading, so columns may
be reordered. Team names come from each row's `data-team-name`; IDs come from
`/teams/{id}` links within the team cell. Participant cards and unrelated tables
are ignored. Every table body row must be valid; missing names/IDs, ambiguous or
duplicate IDs, invalid/overflowing numbers, unexpected columns and empty tables
fail with `FLA_INVALID_RESPONSE`. Error/login pages without the standings
structure fail even with HTTP 200. No response HTML is retained in exception
messages or HTTP exception cause chains that the global handler logs.

Spaces, non-breaking spaces, narrow non-breaking spaces, grouped integers,
explicit plus signs and Unicode minus signs are supported. A dash in the
penalty column means no penalty; dashes in other numeric fields are rejected. Counters must be
nonnegative. Position, penalty, matches played and goal difference are validated
but have no corresponding fields in the existing DTO.

`OpponentMetricsCalculator` already converts unavailable bonus to zero and accepts
empty form lists. The parser explicitly sets `Bonus` to `0` and `CinqDernier` to
`[]`. **Pénalité is not Bonus.** Points come from the primary displayed badge; a separate parenthesized points
annotation is validated but is not concatenated with it. Penalties are not applied
a second time. Form-based metrics consequently have no recent
results and zero streaks. No recent match history is inferred from aggregate
wins/draws/losses. The DTO's legacy JSON names are preserved for saved snapshots
and existing consumers.

Old and new external IDs remain distinct. For example, new team ID `270` is not
translated to old ID `6580`. This change does not update, delete or migrate team
mappings. Existing snapshots remain readable and may be served for their normal
TTL; they are not invalidated by this migration.

## HTML fixtures

`src/test/resources/fla/rankings-16-season-2.html` is the real public HTML page
captured from the new URL on **2026-11-03 UTC**. The session-specific CSRF meta
token was replaced with `REDACTED`; standings markup and data are unchanged.
The parser and client tests verify all **14 teams** can be parsed without
confusing the participant cards with ranking rows.

At capture time YEXIAO PARIS FC / ID 270 has **20 points, 5 wins, 0 draws,
0 losses, 29 goals for, 6 goals against and 0 forfeits**. The live standings have
advanced since the supplied 4-point example. The separate
`rankings-representative.html` fixture retains that example (4 points, 1 win,
4 goals for, 1 goal against), a synthetic second team and participant decoys for
focused whitespace, layout and malformed-row tests. The real page represents no
penalty with a `-`; this notation is accepted only in the penalty column.

Tests use only committed fixtures and never call FLA. A successful parse of this
capture verifies the current markup; future upstream layout changes can still
cause a controlled `FLA_INVALID_RESPONSE`.

## Configuration

The Java property default and `application.yml`, `application-docker.yml`, and
`application-prod.yml` all default to `https://football-loisir-amateur.fr`.
The existing bean also defaults HTML decoding to UTF-8 when the HTTP charset is
omitted; explicit response charsets are still respected.
Connection timeout remains 5 seconds; read timeout remains 20 seconds; snapshot
TTL remains 6 hours (`FLA_SNAPSHOT_TTL` can still override it).

Compose files and example environment files have no FLA base URL override. On
production deployment, remove any old `FLA_BASE_URL` override or set it to
`https://football-loisir-amateur.fr`; check external environment/secrets and direct
`opponent-analysis.fla.base-url` overrides as well. Allow outbound HTTPS to the
new hostname. No database migration or remapping job is required or included.
The legacy `FlaRequestKeyBuilder` and request DTO remain unused; the new client
has no reference to them or `/Home/GetClassement`.

## Automated validation

From the repository root with Java 17 and Maven:

```sh
mvn -pl match-service -Dtest=FlaStandingsHtmlParserTest,FlaClientImplTest,FlaSyncServiceImplTest,FlaStandingsSnapshotServiceImplTest test
mvn -pl match-service test
```

The tests cover parser failures, GET URL/query/header, HTTP status mapping, I/O
and timeout handling, legacy JSON serialization, sync delegation and snapshot
write/read compatibility. The full suite also covers existing cache, repository,
controller, mapping and opponent-analysis behavior.

Validation on Java 17 after implementation:

- Targeted migration/sync/snapshot tests: **81 passed**, zero failures/errors.
- Complete match-service suite: **275 run, 274 passed, 1 failure, zero errors**.
- The sole failure is the existing
  `OpponentAnalysisMetricsControllerTest.exposesOpenApiContract`: it expects the
  OpenAPI tag `Opponent Analysis Metrics`, but the controller declares
  `Opponent Analysis`. The same test failure was reproduced from untouched `HEAD`
  in a temporary directory. Those unrelated files were left unchanged.
- Main and test sources compiled successfully with release 17.

## Manual sync test (local or staging only)

Start the existing local/staging stack with a non-production database and the new
FLA base URL. This POST deliberately fetches live data and writes the local FLA
championship/team cache, so run it manually only when ready. It does not create
standings snapshots; those retain their existing separate refresh flow.

Through the gateway (default port 8088), supply an existing ADMIN or COACH JWT:

```sh
curl --fail-with-body -X POST \
  'http://localhost:8088/api/v1/fla/championnats/16/seasons/2/sync' \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H 'Content-Type: application/json' \
  --data '{"championnatName":"FLA championship 16"}'

curl --fail-with-body \
  'http://localhost:8088/api/v1/fla/championnats/16/seasons/2/teams' \
  -H "Authorization: Bearer ${ACCESS_TOKEN}"
```

For an isolated local match-service on port 8084, its existing trusted user-context
headers can be used instead of the gateway JWT:

```sh
curl --fail-with-body -X POST \
  'http://localhost:8084/api/v1/fla/championnats/16/seasons/2/sync' \
  -H 'X-User-Id: 1' -H 'X-Username: local-admin' -H 'X-User-Roles: ADMIN' \
  -H 'Content-Type: application/json' \
  --data '{"championnatName":"FLA championship 16"}'

curl --fail-with-body \
  'http://localhost:8084/api/v1/fla/championnats/16/seasons/2/teams'
```

Expect HTTP 200 with `championnatId: 16`, `saisonId: 2` and a nonzero
`receivedTeamCount` (14 only if the live page still matches the example).
Check the cached teams include `flaTeamId: 270` / `YEXIAO PARIS FC`. A second sync
should report unchanged teams unless FLA data changed. Existing mappings must
remain untouched. Invalid upstream HTML should return 502 / `FLA_INVALID_RESPONSE`,
upstream failure 502, and timeout 504 / `FLA_TIMEOUT`.

No deployment, production database changes or live synchronization were performed
as part of implementing this migration.
