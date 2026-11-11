# Tournament → Match synchronization

## Flow and contracts

Tournament Service remains authoritative. `updateTournament` keeps its validations, ACTIVE-only update restriction,
transaction and public response. It uses `saveAndFlush` before creating `tournament.updated`, so the payload contains
the persisted JPA version rather than a pre-flush version. The existing Outbox insertion joins the update transaction;
rollback removes both the update and its publishable event. The existing five-second scheduler and synchronous Kafka
send confirmation remain in place.

The existing envelope is unchanged (payload is `data`, not `payload`):

```json
{
  "eventId": "6fc5b4a0-3ab2-4483-8df5-923202cc5541",
  "eventType": "tournament.updated",
  "aggregateType": "tournament",
  "aggregateId": 5,
  "occurredAt": "2026-11-10T12:00:00Z",
  "operator": {"userId": null, "username": "system"},
  "data": {"tournamentId": 5, "name": "Ligue B", "season": "2026/2027", "version": 3}
}
```

The internal `GET /internal/tournaments/{id}/snapshot` response gains an additive `version` field. Existing public
REST paths and request contracts are unchanged. Match creation still fetches the Tournament Snapshot and validates
ACTIVE status, teams and the existing request fields. It additionally coordinates its insert with the sync state lock.
Result, appearance and score business methods are unchanged.

`TournamentKafkaListener` validates event UUID/time, type, aggregate identity, integral IDs/version and complete data.
Valid unrelated Tournament event types are acknowledged without performing synchronization. Modern updates use the
payload directly. Legacy updates containing only `tournamentId` fetch the latest versioned Snapshot through the existing
Feign client. An unversioned response, HTTP failure or mismatched identity cannot overwrite local state; it fails consumption.
Partially populated modern events are invalid, not interpreted as legacy events.

## Atomicity, ordering and creation races

`match.tournament_sync_state` stores the last applied version and metadata. An `INSERT ... ON CONFLICT DO NOTHING`
followed by `SELECT ... FOR UPDATE` safely handles concurrent first use. PostgreSQL holds the row lock through the
service transaction. There are no in-memory correctness locks and no cross-service database writes.

- A newer version updates the Match snapshots, sync state and Match Outbox in one transaction.
- A lower version is ignored.
- The same version and values is a duplicate and does not rewrite Match rows or publish another event.
- The same version with different values fails explicitly and is quarantined by the consumer error handler.
- Reconciliation can repair equal-version drift with the stored authoritative values; it deliberately republishes metadata
  so Statistics can repair its own projections as well. Repeated runs preserve business data.

The JPQL bulk update has **no status or finished filter**. It includes scheduled, ongoing, finished, cancelled and
historical Matches, and only updates rows with different name/season values. It sets `updatedAt` and increments each
changed Match's JPA `version`. All other columns and appearance/result-history rows remain untouched. The modifying
query flushes and clears the persistence context. A concurrent writer holding an older Match version fails optimistic
locking instead of overwriting synchronized metadata; it can retry its original operation against the latest entity.

Creation uses the same lock until its Match and `match.created` Outbox row commit. If a Snapshot HTTP response is older
than an already applied event, creation uses the newer stored metadata. If an event arrives during creation, it waits for
the insert transaction and then includes that Match in its update. If creation observes a newer authoritative version,
it also synchronizes existing Matches. This closes the read-before-event/insert-after-event race.

## Kafka configuration and failure handling

Repository dependency management resolves Spring Boot 4.0.6 and Spring Kafka 4.0.5; production Java target remains 17.
No new production dependency was introduced. `spring-kafka-test` was added only to Match tests, matching Statistics' existing setup.

| Purpose | Topic | Consumer group / key |
| --- | --- | --- |
| Tournament source | `tournament-service.tournament-events` | `match-service-tournament-sync`; key `tournament:<id>` |
| Match events, including committed metadata propagation | `match-service.match-events` | existing `statistics-service`; existing Match keys stay `match:<id>` |
| Tournament consumption quarantine | `tournament-service.tournament-events.DLT` | operational replay |
| Statistics consumption quarantine | `match-service.match-events.DLT` | operational replay |

Match retains `app.kafka.topics.match-events` and adds `app.kafka.topics.tournament-events` in the base configuration,
inherited by Docker/prod. Its dedicated consumer factory does not replace the existing Kafka Producer. Match's Outbox
key now uses `aggregateType:aggregateId`, preserving all existing Match keys and supporting Tournament metadata keys.
The new `tournament.metadata.synchronized` event is published by the existing Match Outbox with aggregate type `tournament`
and data `{tournamentId,name,season,version}`. Audit Service already accepts the generic envelope and requires no change.

The new Match group uses `earliest`: a new group reads all **retained** source events, while an existing group resumes
its committed offsets. This does not recover expired Kafka history; run reconciliation on rollout. Group/reset settings
can be overridden via `APP_TOURNAMENT_SYNC_GROUP_ID` and `APP_TOURNAMENT_SYNC_AUTO_OFFSET_RESET`.

String deserializers and disabled auto-commit are explicit. Both affected listener factories use `MANUAL_IMMEDIATE`:
listeners acknowledge on the consumer thread only after their proxied database service returns with a successful commit.
A process crash between DB commit and offset commit can redeliver; durable version checks make this safe.

Transient failures have one initial attempt plus three retries at one-second intervals. Invalid input is not retried.
After exhaustion, the recoverer publishes the original message and exception headers to `<source-topic>.DLT`, waits for
send confirmation, and only then allows the source offset to advance. Failed DLT publication is not treated as recovery.
A DLT record means **quarantined, not synchronized**. Monitor it, fix the underlying issue, and replay it to the source
or run reconciliation. Recovery cannot guarantee forward progress while both the database and DLT are unavailable;
it retains the source record rather than silently acknowledging an unapplied update.

## Statistics compatibility

Statistics stores name/season in Match summaries, player and team aggregates, appearance contributions and leaderboards.
The metadata handler therefore updates these local projections instead of replaying result/goal/appearance events.
Normal metadata changes preserve scores, goals, assists, appearances, ranking values and leaderboard metrics, and do
not trigger a full statistics recalculation. Existing caches are invalidated only after the projection transaction commits.

A local `statistics.tournament_metadata_state` row serializes metadata refresh with Match event processing. Late Match
events have their tournament name/season normalized from that row before dispatch, preventing old snapshots from
recreating stale season buckets. Lower metadata versions cannot regress projections. Equal-version reconciliation
can repair existing drift; conflicting values at the same version fail.

Existing inconsistencies can have split the same Tournament/player or Tournament/team across several season buckets.
Only those duplicate buckets are consolidated, preserving the **sum of every existing counter**; no new goal, assist,
appearance or result is inferred. Only that Tournament's affected rankings/leaderboards are rebuilt when consolidation
requires it. The common case is a metadata-only bulk update. This preserves stored totals; it cannot diagnose any
unrelated pre-existing double-counting of sporting events.

## Migrations and Docker Compose rollout

The repository uses manually executed SQL for Match and version-named SQL resources for Statistics, with no Flyway
runtime dependency. Do not assume application startup will execute these scripts:

1. `match-service/sql/create-tournament-sync-state.sql`
2. `statistics-service/src/main/resources/db/migration/V8__create_tournament_metadata_state.sql`

Both use `IF NOT EXISTS`, preserve applied progress, and were executed twice successfully on PostgreSQL 16.14.
The Match script also adds an index on `match_info(tournament_id)`. Existing schemas/tables must already exist.
Neither migration changes Tournament tables, existing Match data or historical AI reports.

Using the repository's production Compose convention, execute migrations explicitly before deploying consumers:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T postgres \
  sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 --single-transaction' \
  < match-service/sql/create-tournament-sync-state.sql

docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T postgres \
  sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1 --single-transaction' \
  < statistics-service/src/main/resources/db/migration/V8__create_tournament_metadata_state.sql
```

Provision/check the existing source topics and both `.DLT` topics using the deployment's normal Kafka administration
process. For the current single-broker Compose deployment an example is:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T kafka \
  /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --create --if-not-exists \
  --topic tournament-service.tournament-events.DLT --partitions 1 --replication-factor 1

docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T kafka \
  /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --create --if-not-exists \
  --topic match-service.match-events.DLT --partitions 1 --replication-factor 1
```

The recoverer lets Kafka choose a DLT partition, so DLT partition counts need not match the source. Topic ACLs must allow
Match and Statistics to publish their DLTs. Use normal production replication/retention settings outside single-broker Compose.

Deployment order:

1. Back up and execute the two migrations with the normal deployment database account.
2. Build the affected images with `docker compose -f docker-compose.prod.yml --env-file .env.prod build tournament-service statistics-service match-service`.
3. Deploy Tournament first, so the internal Snapshot API always exposes a version.
4. Deploy Statistics next, so it understands the new metadata event before Match publishes it.
5. Deploy Match, preserving the existing group and topics on later deployments.
6. Run the one-time reconciliation below; monitor Outbox, consumer lag, applied versions and both DLTs.

Do not roll back to an old Statistics consumer while new metadata events are still queued. Coordinate rollback or replay
quarantined records after restoring a compatible consumer. Existing Outbox retry limits remain unchanged and should be monitored.
No production migration, reconciliation, topic creation or service deployment was executed during implementation.

## Controlled reconciliation

There is no new public administrative endpoint and no automatic full-database repair on ordinary startup.
`TournamentReconciliationService` pages through distinct referenced Tournament IDs (100 at a time), retrieves versioned
authoritative Snapshots over HTTP, then invokes a separate transaction for each Tournament. It never holds a DB lock
while waiting for HTTP. A live event with a higher version wins over a stale reconciliation response.

Use a controlled one-shot container with infrastructure/service connectivity already available:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod run --rm --no-deps \
  -e APP_TOURNAMENT_SYNC_RECONCILE=true \
  -e APP_TOURNAMENT_SYNC_RECONCILE_ONLY=true \
  -e SPRING_MAIN_WEB_APPLICATION_TYPE=none \
  -e SPRING_KAFKA_LISTENER_AUTO_STARTUP=false \
  match-service
```

`reconcile-only` closes the application after successful completion; an exception fails startup instead of reporting
success. Prior Tournament transactions remain committed, so retrying the command is safe. A retry starts the ID scan
again; already-correct Match rows are not rewritten. Equal-version metadata is republished through Outbox to repair
Statistics. Do not set these opt-in flags on the normal long-running Compose service.

## Verification

Run the affected regression suites (the local IDE JVM needs a Mockito startup agent):

```bash
mvn -pl tournament-service,match-service,statistics-service -am test -fae \
  -DargLine="-javaagent:$HOME/.m2/repository/org/mockito/mockito-core/5.20.0/mockito-core-5.20.0.jar"
```

Tests cover flushed producer versions; rollback and status restrictions; all Match statuses and historical dates;
unrelated rows and unchanged fields/appearances; duplicate/stale/equal-version events; concurrent first use and
reconciliation; stale optimistic writes; creation races; versioned legacy fallback; missing/malformed data;
acknowledgment ordering; reconciliation pagination and failure; metadata-only Statistics refresh, split-bucket total
conservation, late Match events, transaction rollback and repeatable migrations.

Evidence boundaries:

- H2 integration tests exercise real Spring transactions, JPA bulk updates and Outbox persistence.
- The same three integration classes also ran successfully against a disposable **PostgreSQL 16.14** database:
  18 tests covering producer transactions, Match sync and Statistics projection/migration behavior.
  HTTP lookups remain mocked/test fixtures. The isolated container was removed afterward.
- `TournamentKafkaIntegrationTest` uses a real embedded Kafka broker: a transient DB-service failure is retried,
  a persistent failure exhausts four attempts and reaches DLT, a malformed event reaches DLT, and later messages continue.
  Its sync service is mocked; this is not a full three-service network deployment test.
- No real business database was accessed. No public API path changed. Production Kafka broker 3.7.2 and deployment
  credentials still require the normal post-deployment smoke check; the embedded broker uses the repository's managed test version.
- Compose configuration syntax validation passed. Production environment variables were unavailable for that check,
  so this does not validate production secrets/connectivity. `git diff --check` passed.

Final affected-module results:

| Module | Tests | Passed | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: | ---: |
| Tournament | 8 | 8 | 0 | 0 | 0 |
| Match | 425 | 424 | 1 | 0 | 0 |
| Statistics | 72 | 70 | 0 | 2 | 0 |
| Total | 505 | 502 | 1 | 2 | 0 |

All 34 newly added tests passed. The three existing failures were preserved, not bypassed:

- `OpponentAnalysisMetricsControllerTest.exposesOpenApiContract`: expected Tag `Opponent Analysis Metrics`, actual `Opponent Analysis` (previously known).
- `StatisticsControllerIntegrationTest.dashboardReturnsLatestTournamentSummary` and
  `leaderboardTopNTruncatesResults`: their existing leaderboard fixtures omit the required `appearance_count`.
  The same two errors were reproduced by extracting unmodified HEAD to an isolated `/tmp` directory and executing
  that original test class. The new metadata tests do not alter or relax these assertions/constraints.

To repeat the PostgreSQL verification, use a **disposable test database only**, create the `match` and `tournament`
schemas, and run the three integration classes above with `sync.test.jdbc-url`, `sync.test.jdbc-driver=org.postgresql.Driver`,
`sync.test.jdbc-user` and `sync.test.jdbc-password`. These tests create/drop tables or initialize schemas and must never
be pointed at a business database. The normal default remains H2, without Docker/Testcontainers dependencies.

## Changed files

- Added: `match-service/docs/tournament-synchronization.md`
- Modified: `match-service/pom.xml`
- Added: `match-service/sql/create-tournament-sync-state.sql`
- Added: `match-service/src/main/java/com/yukai/team/matchservice/config/TournamentKafkaConsumerConfig.java`
- Added: `match-service/src/main/java/com/yukai/team/matchservice/config/TournamentReconciliationCommand.java`
- Modified: `match-service/src/main/java/com/yukai/team/matchservice/dto/response/TournamentSnapshotResponse.java`
- Added: `match-service/src/main/java/com/yukai/team/matchservice/kafka/listener/TournamentKafkaListener.java`
- Modified: `match-service/src/main/java/com/yukai/team/matchservice/outbox/OutboxPublisherScheduler.java`
- Modified: `match-service/src/main/java/com/yukai/team/matchservice/repository/MatchInfoRepository.java`
- Added: `match-service/src/main/java/com/yukai/team/matchservice/repository/TournamentSyncStateRepository.java`
- Modified: `match-service/src/main/java/com/yukai/team/matchservice/service/MatchServiceImpl.java`
- Added: `match-service/src/main/java/com/yukai/team/matchservice/service/TournamentReconciliationService.java`
- Added: `match-service/src/main/java/com/yukai/team/matchservice/service/TournamentSyncService.java`
- Added: `match-service/src/main/java/com/yukai/team/matchservice/service/TournamentSyncServiceImpl.java`
- Modified: `match-service/src/main/resources/application.yml`
- Added: `match-service/src/test/java/com/yukai/team/matchservice/config/TournamentReconciliationCommandTest.java`
- Added: `match-service/src/test/java/com/yukai/team/matchservice/kafka/listener/TournamentKafkaIntegrationTest.java`
- Added: `match-service/src/test/java/com/yukai/team/matchservice/kafka/listener/TournamentKafkaListenerTest.java`
- Added: `match-service/src/test/java/com/yukai/team/matchservice/service/MatchCreationTournamentSyncTest.java`
- Modified: `match-service/src/test/java/com/yukai/team/matchservice/service/MatchServiceImplTest.java`
- Added: `match-service/src/test/java/com/yukai/team/matchservice/service/TournamentReconciliationServiceTest.java`
- Added: `match-service/src/test/java/com/yukai/team/matchservice/service/TournamentSyncIntegrationTest.java`
- Modified: `statistics-service/src/main/java/com/yukai/team/statisticsservice/config/KafkaConsumerConfig.java`
- Modified: `statistics-service/src/main/java/com/yukai/team/statisticsservice/entity/enums/StatisticsEventType.java`
- Added: `statistics-service/src/main/java/com/yukai/team/statisticsservice/kafka/handler/TournamentMetadataSynchronizedHandler.java`
- Modified: `statistics-service/src/main/java/com/yukai/team/statisticsservice/service/consumer/StatisticsEventProcessingService.java`
- Added: `statistics-service/src/main/java/com/yukai/team/statisticsservice/service/projection/TournamentMetadataProjectionService.java`
- Added: `statistics-service/src/main/resources/db/migration/V8__create_tournament_metadata_state.sql`
- Modified: `statistics-service/src/test/java/com/yukai/team/statisticsservice/kafka/listener/MatchStatisticsKafkaListenerTest.java`
- Added: `statistics-service/src/test/java/com/yukai/team/statisticsservice/kafka/listener/TournamentMetadataProjectionIntegrationTest.java`
- Modified: `statistics-service/src/test/java/com/yukai/team/statisticsservice/service/consumer/StatisticsEventProcessingServiceTest.java`
- Modified: `statistics-service/src/test/resources/schema.sql`
- Modified: `tournament-service/src/main/java/com/yukai/team/tournamentservice/dto/event/TournamentUpdatedEvent.java`
- Modified: `tournament-service/src/main/java/com/yukai/team/tournamentservice/dto/response/TournamentSnapshotResponse.java`
- Modified: `tournament-service/src/main/java/com/yukai/team/tournamentservice/service/impl/TournamentServiceImpl.java`
- Added: `tournament-service/src/test/java/com/yukai/team/tournamentservice/service/TournamentSynchronizationEventIntegrationTest.java`
