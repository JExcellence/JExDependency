# RaindropCentral Backend — build brief

You are building the HTTP backend that receives data from the **JExCore**
Minecraft plugin. The plugin is already written; this brief is the
contract it uses. Implement the backend exactly to this contract — the
plugin is not changing.

## Context (1 paragraph)

JExCore is a Minecraft/Paper plugin that acts as the shared core library
for a fleet of game servers. Every server runs JExCore and pushes two
kinds of data to your backend: periodic server **heartbeats** (player
counts, TPS, version) and asynchronous **statistic events** (arbitrary
plugin/identifier/value tuples, possibly tied to a player UUID). The
plugin batches, compresses, signs, and retries deliveries. Your backend
ingests, verifies, stores, and exposes the data for a web dashboard.

## Stack — hard constraints

**Use an established, mainstream framework. No Python. No bespoke
frameworks.** Pick one of:

- **Node.js + TypeScript + Fastify (preferred)** + PostgreSQL + Redis
- **Java 21 + Spring Boot 3** + PostgreSQL + Redis
- **Kotlin + Ktor** + PostgreSQL + Redis
- **Go + Gin or Echo** + PostgreSQL + Redis
- **Rust + Axum** + PostgreSQL + Redis

Do **not** use: Python/FastAPI/Flask/Django, Deno/Bun-only runtimes,
hand-rolled HTTP frameworks, experimental ORMs, or anything that isn't
battle-tested in production.

For persistence: use the stack's mainstream ORM/query builder (Prisma
or Drizzle for Node, JPA/Hibernate for Spring, Exposed for Ktor, sqlx
for Rust, sqlc or GORM for Go). Raw SQL with migrations is also fine.

For schema migrations: use the stack's mainstream migration tool
(Prisma Migrate, Flyway, Liquibase, golang-migrate, sqlx-cli). Every
schema change lands as a versioned migration file.

Provide a `docker-compose.yml` that brings up the API, DB, and cache
in one command.

## Data model (PostgreSQL)

```
server(
  id              bigserial primary key,
  server_uuid     uuid unique not null,
  owner_user_id   bigint references user(id),
  api_key_hash    text not null,          -- bcrypt or argon2 of the bearer token
  hmac_secret     text not null,          -- random 32+ bytes, stored encrypted at rest
  display_name    text,
  registered_at   timestamptz not null default now(),
  revoked_at      timestamptz
)

server_heartbeat(
  server_id       bigint not null references server(id) on delete cascade,
  at              timestamptz not null,
  current_players int  not null,
  max_players     int  not null,
  tps             real not null,
  server_version  text,
  plugin_version  text,
  primary key (server_id, at)
) partition by range (at);           -- monthly partitions

batch(
  batch_id        uuid primary key,    -- client-generated; used for idempotent retries
  server_id       bigint not null references server(id),
  received_at     timestamptz not null default now(),
  entry_count     int not null,
  bytes           int not null
)

stat_entry(
  id              bigserial primary key,
  batch_id        uuid not null references batch(batch_id),
  server_id       bigint not null,
  plugin          text not null,
  identifier      text not null,
  player_id       uuid,
  value_type      text not null check (value_type in ('number','boolean','string','date')),
  value_number    double precision,
  value_boolean   boolean,
  value_string    text,
  value_date_ms   bigint,
  attributes      jsonb not null default '{}',
  event_ts        timestamptz not null,
  priority        text not null check (priority in ('LOW','NORMAL','HIGH','CRITICAL'))
)

create index stat_entry_server_player on stat_entry(server_id, player_id);
create index stat_entry_plugin_id     on stat_entry(plugin, identifier);
create index stat_entry_event_ts      on stat_entry(event_ts desc);

user(
  id              bigserial primary key,
  email           citext unique not null,
  password_hash   text not null,
  created_at      timestamptz not null default now()
)
```

## Endpoints

All endpoints use JSON unless stated otherwise.

### `POST /api/v1/heartbeat`
Called every 60s by each server.

**Auth:** `Authorization: Bearer <apiKey>` (verify against `server.api_key_hash`).

**Body:**
```json
{
  "serverUuid": "uuid",
  "at": "2026-04-20T16:00:00Z",
  "currentPlayers": 12,
  "maxPlayers": 100,
  "tps": 19.8,
  "serverVersion": "Paper 1.21.3",
  "pluginVersion": "1.0.0"
}
```

**Response:** `204 No Content` on success. `401` on bad key. `410 Gone`
if the server is revoked. Insert a row into `server_heartbeat`.

### `POST /api/v1/stats/ingest`
Called by the plugin's `StatisticsDelivery` engine.

**Auth:** `Authorization: Bearer <apiKey>` on `server.api_key_hash`.

**Headers (all required except `X-JExCore-Signature` which is optional):**
```
Content-Type:       application/json
Content-Encoding:   gzip
X-JExCore-Batch-Id: <uuid>
X-JExCore-Server:   <uuid>
X-JExCore-Signature: <hex HMAC-SHA256 of the gzipped body>
```

**Body (after gunzip):**
```json
{
  "batchId": "f1e4...-uuid",
  "serverUuid": "uuid",
  "createdAt": "2026-04-20T16:00:00Z",
  "entries": [
    {
      "plugin": "mygame",
      "identifier": "blocks_broken",
      "playerId": "uuid-or-null",
      "value": 42,                      // number | boolean | string | epochMillis
      "attributes": { "world": "overworld" },
      "timestamp": "2026-04-20T15:59:58Z",
      "priority": "NORMAL"              // LOW | NORMAL | HIGH | CRITICAL
    }
  ]
}
```

**Processing:**
1. Match `Authorization` to a server; reject 401 if unknown.
2. If `X-JExCore-Signature` is present, verify HMAC-SHA256 of the raw
   gzipped body against `server.hmac_secret`. Mismatch → 401.
3. De-dupe: if `batch_id` already exists for this server, return `200`
   with `{ "status": "duplicate" }` and do not insert again.
4. Validate schema; malformed entries → 422 with error detail, no rows
   inserted.
5. Insert `batch` + N `stat_entry` rows in a single transaction.
6. Classify `value` into one of the four typed columns based on JSON
   type (`number` → `value_number`, etc.). Epoch-millis longs go to
   `value_date_ms` only when `value` is a `long`-shaped number AND the
   entry's identifier has suffix `.ts` OR attributes contain
   `"type": "date"` — otherwise treat as `value_number`.
7. Return `202 Accepted` with `{ "status": "accepted", "inserted": N }`.

**Retry semantics:** the plugin retries `5xx` and timeouts with
exponential backoff. It treats `400`, `401`, `403`, `422` as fatal (no
retry) and spools them to disk. So: use `4xx` only for caller errors,
`5xx` for transient server issues.

### `POST /api/v1/servers/register`
Owner creates a new server and receives credentials.

**Auth:** user session cookie (owner dashboard).

**Body:** `{ "displayName": "string" }`

**Response:**
```json
{
  "serverUuid": "uuid",
  "apiKey": "shown-once-plaintext-token",
  "hmacSecret": "shown-once-plaintext-32byte-hex"
}
```
Persist only `api_key_hash` (bcrypt/argon2id) and `hmac_secret`
(encrypted at rest with an application key in env var
`APP_ENCRYPTION_KEY`). The server admin copies these into their
plugin's `config.yml`.

### `POST /api/v1/servers/:id/rotate`
Rotates the api key and/or HMAC secret. Returns the same shape as
`/register`. Server must be re-configured with the new values.

### `GET /api/v1/servers`
Owner dashboard — lists the caller's servers with the latest heartbeat
timestamp, online player count, status (`online` if heartbeat within
last 120s, else `offline`).

### `GET /api/v1/servers/:id/stats`
Query statistics.

**Query params:**
- `plugin` (required)
- `identifier` (required)
- `playerId` (optional uuid)
- `since`, `until` (iso8601, default: last 7d)
- `limit` (default 500, max 10000)
- `aggregate` = `none|sum|avg|max|count` (default `none`)

**Response:** `[ { "ts": "...", "value": 42, "attributes": {...} }, ... ]`
or `{ "aggregate": "sum", "value": 12345 }`.

### `GET /api/v1/servers/:id/leaderboard`
**Query:** `plugin`, `identifier`, `limit` (default 10).

Returns top-N `playerId`s by latest numeric value.

### `GET /api/v1/healthz`
Unauthenticated liveness check. Return `200` `{"status":"ok"}` when the
DB is reachable.

## Rate limiting

- **Per api key:** 60 heartbeats/min, 120 ingest requests/min, 200 query
  requests/min. Token bucket in Redis. Exceed → `429` with
  `Retry-After` header.
- **Global ingest:** 2000 req/s. Drop excess with `503`.

The plugin respects `Retry-After`.

## Security

- TLS required in production; reject HTTP in non-dev environments.
- Validate `Content-Length` before gunzipping (cap at 8 MiB raw / 64 MiB
  decompressed) to prevent zip-bombs.
- Verify HMAC (when present) using a **constant-time comparator**.
- Never log api keys, bearer tokens, or hmac secrets, even at `DEBUG`.
- On `Authorization: Bearer` mismatch, always return the same latency
  and body to avoid timing oracles.
- `player_id` is sensitive — treat like PII. Any deletion request (by
  uuid) must cascade through `stat_entry`.

## Idempotency

Every ingest request carries a client-generated `batchId`. Retries use
the same id. Your backend **must** de-duplicate on insert — return
`200 {"status":"duplicate"}` without re-inserting. This is the core
guarantee the plugin depends on to recover from network failures without
counting events twice.

## Tests you must ship

- Round-trip: POST one batch, verify rows in DB, POST same batch again,
  verify no duplicates.
- HMAC: valid signature passes, single-byte flip rejects with 401.
- Auth: unknown bearer → 401; revoked server → 410; valid → 202.
- Schema: missing field → 422 with actionable error; oversized payload
  → 413.
- Heartbeat: 60s-old row shows `online`, 180s-old shows `offline`.
- Rate limit: burst of 200 requests against a 120/min key gets 80
  `429`s with `Retry-After`.
- Legacy migration: seed a fixture DB with the RCore schema + 100 of
  each entity; run `migrate` against a fresh target DB; verify every
  mapping row; run it again and assert no duplicates appear.

## Deliverables

1. Source tree with one-command `docker compose up`.
2. `README.md` with `.env` variables, migration step, first-run script
   to create a root user.
3. OpenAPI 3.1 spec at `/openapi.json` served by the API.
4. Structured JSON logs (one line per request) with correlation IDs.
5. A minimal web dashboard (React or HTMX — pick one, keep it single-page)
   showing: servers list, heartbeat freshness, a stats chart view, a
   server-registration flow.

## Legacy RCore → JExCore data migration

An existing prod deployment runs the **RCore** plugin (JExCore's
predecessor) with its own database. On cutover, that data must move
into your new tables. The backend **must ship** a one-shot migration
command (a CLI subcommand or a `/api/v1/admin/migrate` endpoint gated
by an admin token) that reads from the legacy schema and writes the
new schema in one transaction per logical unit.

### Legacy schema (source — don't modify, read-only)

All tables live in the old plugin's MySQL/H2 database:

```
r_player(
  id               bigint pk,
  unique_id        binary(16)    -- UUID stored as 16 bytes (UuidBytesConverter)
  player_name      varchar(16),
  first_seen       datetime,
  last_seen        datetime
)

r_central_server(
  id                                   bigint pk,
  server_uuid                          uuid,
  owner_minecraft_uuid                 varchar,
  api_key_hash                         varchar(60),      -- bcrypt
  connection_status                    enum('CONNECTED','DISCONNECTED','ERROR'),
  last_heartbeat                       datetime,
  current_players                      int,
  max_players                          int,
  tps                                  double,
  server_version                       varchar(50),
  plugin_version                       varchar(20),
  droplet_store_allowed_item_codes_json longtext,
  droplet_store_allowed_item_codes_fetched_at datetime,
  is_public                            boolean,
  share_player_list                    boolean,
  share_metrics                        boolean,
  failed_heartbeat_count               int,
  first_connected_at                   datetime,
  api_key_displayed_until              datetime
)

r_player_servers(player_id bigint, server_id bigint)  -- m2m join

r_player_inventory(
  id            bigint pk,
  player_id     bigint fk → r_player,
  server_id     bigint fk → r_central_server,
  inventory     longtext,    -- ItemStackSlotMap serialisation
  armor_contents longtext,
  enderchest    longtext
)

r_boss_bar_preference(
  id           bigint pk,
  player_uuid  binary(16),
  provider_key varchar(64),
  enabled      boolean,
  unique (player_uuid, provider_key)
)

r_boss_bar_preference_option(
  id            bigint pk,
  preference_id bigint fk,
  option_key    varchar(64),
  option_value  varchar(128),
  unique (preference_id, option_key)
)

r_player_statistic(
  id         bigint pk,
  server_id  bigint fk,
  -- the owning RPlayer is resolved by r_player.playerStatistic_id
)

r_statistic(
  id                   bigint pk,
  identifier           varchar(100),
  plugin               varchar(50),
  statistic_type       varchar,       -- discriminator: BOOLEAN | NUMBER | DATE | STRING
  statistic_boolean    boolean null,
  statistic_number     double null,
  statistic_date       bigint null,   -- epoch millis
  statistic_string     text null,
  player_statistic_id  bigint fk,
  unique (identifier, player_statistic_id)
)
```

### Target schema (destination)

Your own tables as defined above: `server`, `server_heartbeat`, `batch`,
`stat_entry`, `user`, plus two new tables for migrated player-scoped
data that doesn't fit the event-stream model:

```
core_player(
  id             bigserial pk,
  unique_id      uuid unique not null,
  player_name    varchar(16) not null,
  first_seen     timestamptz not null,
  last_seen      timestamptz not null
)

core_player_inventory(
  id             bigserial pk,
  player_id      bigint fk → core_player,
  server_id      bigint fk → server,
  inventory      text,            -- opaque; do not reinterpret
  armor          text,
  enderchest     text,
  captured_at    timestamptz not null default now()
)

core_boss_bar_preference(
  id             bigserial pk,
  player_uuid    uuid not null,
  provider_key   varchar(64) not null,
  enabled        boolean not null,
  options        jsonb not null default '{}',    -- fold r_boss_bar_preference_option in
  unique (player_uuid, provider_key)
)
```

Historical statistics from `r_statistic` fold into your `stat_entry`
event stream — one row per legacy statistic value, with
`event_ts = r_player.last_seen` (best available proxy),
`priority = 'NORMAL'`, `batch_id` = a fresh uuid per migration batch,
and `value_type` mapped from the discriminator.

### Mapping rules (exact)

| Legacy column | Target column | Transform |
|---|---|---|
| `r_player.unique_id` (binary(16)) | `core_player.unique_id` | decode 16-byte big-endian to uuid |
| `r_player.player_name` | `core_player.player_name` | verbatim, trim to 16 chars |
| `r_player.first_seen` / `last_seen` (naive) | `core_player.*` | assume UTC, convert to `timestamptz` |
| `r_central_server.server_uuid` | `server.server_uuid` | verbatim |
| `r_central_server.api_key_hash` | `server.api_key_hash` | verbatim (already bcrypt) |
| `r_central_server.connection_status` | discarded | runtime-only state |
| `r_central_server.last_heartbeat` + player counts + tps | one row in `server_heartbeat` at `last_heartbeat` | last known values |
| `r_central_server.droplet_store_*` | discarded | plugin-specific gameplay, not core |
| `r_player_inventory.*` | `core_player_inventory.*` | opaque blob passthrough |
| `r_boss_bar_preference` + `r_boss_bar_preference_option` | one row in `core_boss_bar_preference` with `options` jsonb aggregating option rows | group by `preference_id` |
| `r_statistic.statistic_type=BOOLEAN` | `stat_entry.value_type='boolean'`, `value_boolean` from `statistic_boolean` | — |
| `…=NUMBER` | `value_type='number'`, `value_number` from `statistic_number` | — |
| `…=STRING` | `value_type='string'`, `value_string` from `statistic_string` | — |
| `…=DATE` | `value_type='date'`, `value_date_ms` from `statistic_date` | — |
| `r_statistic.identifier`, `plugin` | `stat_entry.identifier`, `plugin` | verbatim |
| `r_player_statistic → r_player` | `stat_entry.player_id` | resolve via join |
| owning server | `stat_entry.server_id` | resolve via `r_player_statistic.server_id → server.id` |

### Migration process requirements

- **Idempotent**: running the migration twice produces the same final
  state. Use `ON CONFLICT DO NOTHING` (or equivalent) keyed on the
  natural keys above. Log a summary of inserted / skipped / failed
  rows.
- **Transactional per table group**: `core_player` in one tx,
  inventories in one tx, boss-bars in one tx, statistics in chunked
  tx's of 10 000 rows. Do not wrap the whole migration in one giant
  transaction.
- **Dry-run flag**: support `--dry-run` which computes counts and
  validates mappings without writing.
- **Source connection configurable**: legacy DB URL via
  `LEGACY_DB_URL` env var (JDBC-style or driver-appropriate); default
  off → migration command errors out cleanly if unset.
- **Resume-safe**: if interrupted, rerunning picks up where it left
  off (the idempotent keys take care of this).
- **No plugin changes**: the JExCore plugin is frozen; it will not
  read legacy tables. Migration is a backend-only concern.

### What NOT to migrate

- `r_central_server.droplet_store_*` columns — plugin-specific
  gameplay, not core.
- `connection_status` — runtime state, always re-derived from
  heartbeat freshness.
- `failed_heartbeat_count` — runtime state.
- Anything in tables named `droplet_*`, `cookie_*`, or with a
  `statistic_type='VANILLA_*'` discriminator — out of scope.

### Cutover sequence (document in README)

1. Put the game servers in maintenance mode.
2. Stop the old RCore plugin on all servers.
3. Run `migrate --dry-run` on a staging copy of the legacy DB.
4. Run `migrate` against the production legacy DB pointing at the new
   backend's target DB.
5. Issue fresh `apiKey` + `hmacSecret` per server via
   `/api/v1/servers/register` (old `api_key_hash` is preserved for
   reference but new bearer tokens are minted).
6. Deploy JExCore shadow JARs with the new credentials in config.yml.
7. Start servers; verify heartbeats arrive and `/api/v1/servers` lists
   them online.

## Non-goals (do not build)

- Authentication federation / SSO / OAuth providers.
- Downstream analytics warehouse (ClickHouse / BigQuery etc.) — plain
  Postgres is the target; aggregate queries use server-side SQL.
- Plugin distribution / auto-updater.
- Anything in the plugin itself — it's done and frozen.

## Success criteria

- `docker compose up` on a fresh machine gives a working API within 60s.
- The JExCore plugin, configured with the emitted `apiKey` + `hmacSecret`
  and `stats.endpoint` pointed at `https://<host>/api/v1/stats/ingest`,
  delivers a batch and it appears in `stat_entry` within 1 second.
- The dashboard lists the server as `online` after the first heartbeat.
- All tests in the "Tests you must ship" section pass.

## Start

Begin with the DB migrations + `healthz`, then `/servers/register` so
you can generate credentials, then `/heartbeat`, then `/stats/ingest`
(the hardest one — batch the idempotency + HMAC + schema path
carefully), then queries, then the dashboard, then the **legacy RCore
migration command** last (it's a one-shot tool, not a hot path). Show
the OpenAPI spec at the very end.

Do not skip the retry-semantics table above — the plugin will
misbehave if `4xx` / `5xx` are confused. Do not skip the mapping table
in the migration section — the cutover will silently drop data if
columns are misaligned.
