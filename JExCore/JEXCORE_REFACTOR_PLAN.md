# JExCore Refactor Plan — port RCore to a clean JExcellence-stack module

## Context

`JExCore` is a greenfield plugin replacing the legacy `RCore`. Source of
truth is `JExEconomy` — same module shape, same stack, same conventions.
`RCore` is read-only input: every class is **re-implemented cleanly**, not
moved or patched in place. The `JExCore` aggregate has already been
scaffolded with four modules (`jexcore-api`, `jexcore-common`,
`jexcore-free`, `jexcore-premium`), a `JExCore` orchestrator, edition
delegates, `paper-plugin.yml` per edition, `en_US.yml`, and the Hibernate
defaults. That scaffold is Phase 1 — all subsequent phases port features
into it.

### RCore inventory (input)

- **132 main / 25 test Java files** under `RCore/rcore-common/src/**`
- 4 entity sub-packages (central, inventory, player, statistic) — **11
  entities**, **6 repositories**, **8 top-level services**, **13
  central-subsystem files** (7 + 6 cookie), **~70 statistics files** across
  13 sub-packages, **4 proxy**, **1 velocity**, **3 listeners**, **7 views**,
  **3 command files** (legacy `PRC` monolith + E-prefix enum), **4 config**,
  **1 util**, 1 public-API adapter
- **34 translation YAMLs**, 7 other resource trees (commands, configs,
  database, dependency, logs, proxy, rcentral)
- **Forbidden markers to drop on re-implementation:** Lombok (62 files),
  `rplatform`, `CentralLogger`, `R18nBuilder`, `--enable-preview`,
  `ERCentralPermission` (E-prefix), old `PRC` dispatch pattern.

### JExCore target stack (floor)

| Dep | Version | Source |
|---|---|---|
| Paper API | 1.21 | `libs.paper.api` |
| JEHibernate (thin, bundled) | 3.0.2 | `libs.jehibernate` |
| Hibernate ORM | 7.1.4.Final | `libs.bundles.hibernate` |
| Reflections | 0.10.2 | `RuntimeDependencies.kt` |
| JExPlatform | current | `libs.jexplatform` |
| JExCommand | 2.0 | `libs.bundles.jexcellence` |
| JExTranslate / R18nManager | current | `libs.bundles.jexcellence` |
| InventoryFramework | 3.7.1 | `libs.bundles.inventory` |
| AnvilInputFeature | bundled | `libs.bundles.inventory` |
| Jackson 3.x | 2.18.2 + `tools.jackson` | `libs.jackson.*` |
| Adventure / MiniMessage | 4.17.0 | `libs.adventure.*` |

## Architecture — target shape

```
JExCore/
  build.gradle.kts                       (aggregate, buildAll + publishLocal)
  jexcore-api/                           (public SPI — third-party consumers)
     de.jexcellence.core.api.*           JExCoreAPI, CorePlayerSnapshot,
                                         BossBarProvider, events
  jexcore-common/                        (library — all logic)
     de.jexcellence.core.JExCore         orchestrator (already in place)
     de.jexcellence.core.command.**      JExCommand 2.0 handler classes
     de.jexcellence.core.database.**     entities + repositories
     de.jexcellence.core.service.**      concrete services on ServicesManager
     de.jexcellence.core.listener.**     Bukkit listeners
     de.jexcellence.core.view.**         InventoryFramework views
     de.jexcellence.core.integration.**  PlaceholderAPI / Vault hooks
     de.jexcellence.core.config.**       config sections (JEConfig)
     resources/
       commands/*.yml                    JExCommand 2.0 trees
       translations/en_US.yml            single locale file
       database/, logs/                  defaults
  jexcore-free/                          edition delegate + paper-plugin.yml
  jexcore-premium/                       edition delegate + paper-plugin.yml
```

Proxy / Velocity code from RCore is **not** ported — out-of-scope for a
single-process plugin core. If a multi-server deployment needs it later, it
lands as a separate `jexcore-proxy` sibling module, not bundled in common.

## Phase list

Each phase: ≤15 touched files unless mechanical, ends with
`./gradlew :JExCore:buildAll` green, one decision entry appended to
`.dual-graph/context-store.json`. Old-RCore file → new-JExCore file mapping
is given in every table. Names drop the `R` prefix and switch to the
`de.jexcellence.core` package root. E-prefix enums are renamed to plain
names. Lombok is stripped; every re-implemented class is hand-written with
explicit getters/setters (records where the data never mutates).

### Phase 1 — Scaffold & build graph (done)
4 modules, orchestrator, delegates, `paper-plugin.yml`, en_US.yml baseline,
Hibernate defaults. Confirmed green: `:JExCore:buildAll` produces
`JExCore-1.0.0-Free.jar` + `JExCore-1.0.0-Premium.jar`.

### Phase 2 — Entities
| RCore old | JExCore new |
|---|---|
| `database/entity/player/RPlayer` | `database/entity/CorePlayer` (already started) |
| `database/entity/central/RCentralServer` | `database/entity/CentralServer` |
| `database/entity/inventory/RPlayerInventory` | `database/entity/PlayerInventory` |
| `database/entity/player/RBossBarPreference` | `database/entity/BossBarPreference` |
| `database/entity/player/RBossBarPreferenceOption` | `database/entity/BossBarPreferenceOption` |
| `database/entity/statistic/RAbstractStatistic` | `database/entity/statistic/AbstractStatistic` |
| `database/entity/statistic/RPlayerStatistic` | `database/entity/statistic/PlayerStatistic` |
| `database/entity/statistic/RBoolean/Date/Number/String/Statistic` | `…/Boolean/Date/Number/String/Statistic` |

All extend `LongIdEntity`. No Lombok, no behavior. 11 entities + 3 `package-info.java`.

### Phase 3 — Repositories
6 repos → `AbstractCrudRepository<Entity, Long>`:
`CorePlayerRepository`, `CentralServerRepository`, `PlayerInventoryRepository`,
`BossBarPreferenceRepository`, `PlayerStatisticRepository`, `StatisticRepository`.
Async `CompletableFuture<...>` queries. 6 repos + 1 `package-info`.

### Phase 4 — Core services
| RCore | JExCore |
|---|---|
| `RCoreService` | `CoreService` |
| `RPlayerService` | `CorePlayerService` |
| `RCoreBossBarService` + `RCoreBossBarManager` | `BossBarService` (merged) |
| `InventoryService` | `PlayerInventoryService` |
| `RCentralServerService` | `CentralServerService` |
| `RPlayerStatisticService` + `RStatisticService` | `StatisticService` (merged) |

Concrete classes registered on `Bukkit.getServicesManager()` in
`JExCore#registerServices`, unregistered in `onDisable`. Every
`CompletableFuture` terminates in `.exceptionally(...)`. 6 services + 1
`package-info`.

### Phase 5 — Central subsystem (RaindropCentral integration)
| RCore | JExCore |
|---|---|
| `service/central/RCentralService` | `service/central/CentralService` |
| `service/central/RCentralApiClient` | `service/central/CentralApiClient` |
| `service/central/HeartbeatScheduler` | `service/central/HeartbeatScheduler` |
| `service/central/MetricsCollector` + `MetricsTrackingService` | `service/central/MetricsService` (merged) |
| `service/central/DropletClaimService` | `service/central/DropletClaimService` |
| `service/central/ServerContext` | `service/central/ServerContext` (record) |
| `service/central/cookie/**` (6) | `service/central/cookie/**` (dropped `Droplet` prefix — remain 6) |

Logging via `platform.logger()`; no `CentralLogger`. HTTP retries use
JExPlatform scheduler. ~13 files + 1 `package-info`. Phase may split if
it exceeds 15.

### Phase 6 — Statistics subsystem (3 sub-batches)

**6a — Core + delivery** (~16 files → split 8+8 if needed):
`StatisticsDeliveryService(Factory)`, `delivery/**` (13 classes),
`aggregation/**` (2).

**6b — Collectors & queue** (~14 files):
`collector/**` (5), `queue/**` (6), `offline/ConnectivityManager`,
`monitoring/**` (3).

**6c — Vanilla statistics** (~24 files → split):
`vanilla/**` including `collector/**` (6), `scheduler/**` (2),
`aggregation/**` (1), `batch/**` (1), `cache/**` (1), `config/**` (1),
`event/**` (1), `monitoring/**` (3), `privacy/**` (2), `sync/**` (1),
`version/**` (3), plus root 5.

Security package (`PayloadSigner`, `SensitiveDataEncryptor`,
`StatisticSanitizer`) and `sync/**` folded into 6a.
`StatisticsDeliveryCommand` + `VanillaStatisticsCommand` deferred to
Phase 8 (commands).

### Phase 7 — Listeners
| RCore | JExCore |
|---|---|
| `listener/PlayerJoinLeaveListener` | `listener/PlayerLifecycleListener` |
| `listener/DropletCookieListener` | `listener/CookieListener` |
| `listener/DropletDoubleDropListener` | `listener/DoubleDropListener` |

All events go through `CorePlayerService.findOrCreateAsync` on
`AsyncPlayerPreLoginEvent` (mirrors `PlayerJoinListener` from JExEconomy).
No hard-coded MiniMessage. 3 files + 1 `package-info`.

### Phase 8 — Commands (JExCommand 2.0)
Collapse `PRC` + `PRCSection` + `ERCentralPermission` into a handler tree:

| RCore leaf (legacy dispatch) | JExCore YAML + handler |
|---|---|
| `/prc` root | `commands/jexcore.yml` → `CoreHandler` |
| `/prc reload` | `commands/jexcore.yml` subcommand → `CoreHandler#onReload` |
| `/prc info` / `version` | `CoreHandler#onInfo` |
| `/prc player <name>` | `CoreHandler#onPlayer` |
| `/prc bossbar` | `commands/bossbar.yml` → `BossBarHandler` |
| `/prc stats` | `commands/stats.yml` → `StatisticsHandler` (absorbs `StatisticsDeliveryCommand` + `VanillaStatisticsCommand`) |

Permissions live in YAML. `R18nCommandMessages` bridge added. Handler
files: `CoreHandler`, `BossBarHandler`, `StatisticsHandler`,
`R18nCommandMessages`, argument types as needed. 3 YAMLs + 5–7 Java files.

### Phase 9 — Views (InventoryFramework)
| RCore | JExCore |
|---|---|
| `view/RCoreMainOverviewView` | `view/CoreOverviewView` |
| `view/RCoreMainModule` | merged into `CoreOverviewView` (helper is overkill) |
| `view/RCoreBossBarOverviewView` | `view/BossBarOverviewView` |
| `view/RCoreBossBarProviderView` | `view/BossBarProviderView` |
| `view/DropletClaimsView` | `view/ClaimsView` |
| `view/DropletJobSelectionView` | `view/JobSelectionView` |
| `view/DropletSkillSelectionView` | `view/SkillSelectionView` |

All subclass `View` (or `PaginatedView` / `AnvilInputFeature` as needed).
Registered on `BukkitViewFrame` in `JExCore#registerViews`. Lore capped at
3 lines: description → blank → cyan action prompt. 6 views + 1
`package-info`.

### Phase 10 — Config
| RCore | JExCore |
|---|---|
| `config/RCentralConfig` | `config/CentralConfig` |
| `config/RCentralSection` | `config/CentralSection` |
| `config/RCoreMainMenuConfig` | `config/CoreMenuConfig` |
| `config/RCoreMainMenuConfigLoader` | `config/CoreMenuConfigLoader` |
| `service/statistics/config/StatisticsDeliveryConfig` | `config/StatisticsConfig` |
| `service/statistics/vanilla/config/VanillaStatisticConfig` | `config/VanillaStatisticsConfig` |

Uses JEConfig section loaders consistently. 6 files + 1 `package-info`.
Resource YAMLs (`configs/config.yml`, `rcentral/rcentral.yml`,
`statistics-delivery-config.yml`, `proxy/proxy.yml` — dropped) copied into
common resources.

### Phase 11 — Translations
Single `resources/translations/en_US.yml`. Merge every user-visible key
used in RCore (scanned from source) into the JExCore key tree using the
mandated gradient palette and prefixes (`✔ / ✘ / ⚠ / » / ▸`). Drop
33 legacy-locale YAMLs. 1 file (rewrite).

### Phase 12 — API module (jexcore-api)
Mirror `jexeconomy-api`:
- `JExCoreAPI` (already scaffolded — extend with `corePlayerSnapshot(uuid)`,
  `bossBar()`, `statistics()` accessors)
- `CorePlayerSnapshot` (record)
- `BossBarProvider` (interface)
- `StatisticsProvider` (interface)
- `event/PlayerTrackedEvent`, `event/PlayerUntrackedEvent`,
  `event/BossBarUpdateEvent`

4 records/interfaces + 3 events + `package-info`. Registered on
`ServicesManager` under `JExCoreAPI.class` in Phase 4.

### Phase 13 — Wiring & cleanup
Extend `JExCore#onEnable` pipeline:
`initializeDatabase → initializeServices → registerIntegrations →
registerListeners → registerViews → registerCommands`.
Each method mirrors its JExEconomy counterpart. Add `registerIntegrations`
(PAPI, Vault-style registration if applicable). Add `unregisterIntegrations`
in `onDisable`. Ensure every created `CompletableFuture` has an
`.exceptionally`. Add missing `package-info.java` files across every
package.

### Phase 14 — Tests
Port the 25 RCore tests that still apply (skip E-prefix enum test, skip
old PRC tests). Target: value-object tests (`DeliveryReceipt`, `BatchPayload`,
`AggregatedStatistics`, etc.), `RateLimiter`, `ActiveCookieBoost`,
`QueuePersistenceManager`. Tests go to
`jexcore-common/src/test/java/de/jexcellence/core/**`. Any that depend on
RCore-only types (PRC, proxy) are dropped.

### Phase 15 — Verification
- `./gradlew :JExCore:buildAll` green.
- `./gradlew :JExCore:jexcore-common:checkstyleMain` green (warnings=0).
- `./gradlew :JExCore:jexcore-common:javadoc` green.
- Grep guardrails across `JExCore/**/src/**` — all zero hits:
  `lombok`, `--enable-preview`, `\bE[A-Z]\w+Type\b`, `\bE[A-Z]\w+Permission\b`,
  `rplatform`, `CentralLogger`, `R18nBuilder`, `%\w+%` (old placeholder
  syntax in YAML).
- Every `de.jexcellence.core.**` package has a `package-info.java`.
- Refresh `CONTEXT.md`.

## File-count summary

| Phase | Created | Rewritten | Deleted-from-input | Notes |
|---|---|---|---|---|
| 1 | 15 | — | — | scaffold (done) |
| 2 | 12 | ↔ 11 entities | — | |
| 3 | 7 | ↔ 6 repos | — | |
| 4 | 7 | ↔ 8 services merged to 6 | 2 | |
| 5 | 14 | ↔ 13 central | 1 (merged) | split if >15 |
| 6a | 16 | ↔ 16 | — | |
| 6b | 14 | ↔ 14 | — | |
| 6c | 24 | ↔ 24 | — | split into 2 ≤12 sub-batches |
| 7 | 4 | ↔ 3 | — | |
| 8 | 10 | ↔ 3 + 2 stats commands | 1 (E-enum) | |
| 9 | 7 | ↔ 7 | 1 (module helper merged) | |
| 10 | 7 | ↔ 6 | — | |
| 11 | 1 | ↔ 1 | 33 locales | |
| 12 | 8 | — | — | |
| 13 | — | (orchestrator) | — | |
| 14 | ~15 | ↔ 25 | 10 (obsolete) | |
| 15 | — | — | — | grep-only |

## Dependency graph (phase order is load-bearing)

```
1 (done) → 2 → 3 → 4 ─┬─→ 7 ─┐
                      ├─→ 5 ─┤
                      ├─→ 6a→6b→6c ─┤
                      ├─→ 9  ─┤
                      ├─→ 10 ─┤
                      ├─→ 12 ─┤ → 13 → 14 → 15
                      └─→ 11 ─┘
                              8 requires 4+5+6+11
```

Phases 5/6/7/9/10/11/12 are parallel branches once Phase 4 (services) is
green. Phase 8 (commands) needs services + stats + translations. Phases
13–15 close the loop.

## Risks

- **Statistics subsystem size.** 70+ files touched; Phase 6 must split
  aggressively. Any sub-batch >12 files is a smell — split further.
- **Proxy / Velocity omission.** If downstream servers actually need
  cross-server messaging, Phase 7 must be re-added as its own module.
  Currently out of scope — flag if anyone asks.
- **Central API (HTTP) contract.** The `RCentralApiClient` talks to an
  external service whose schema is unknown here; re-implementation must
  preserve wire format. Read the old JSON fixtures before Phase 5.
- **Gradle `ConstantsTreeVisitor` NPE** (hit during scaffold): avoid
  `private static final String EDITION = "..."` captured by anonymous
  subclasses — inline literals instead. Already applied in delegates.
- **`implementation(project(":jexcore-api"))` in common is module-private.**
  Free/Premium must also declare `implementation(project(":JExCore:jexcore-api"))`
  (already applied) — any new subproject needs it too.
- **Checkstyle `maxWarnings=0` + Javadoc `-Werror`.** Every public type
  needs a complete Javadoc block. Cost of each phase is higher than pure
  logic; budget accordingly.

## Note on verbosity (adam-the-developer reference)

Per the referenced article — "Java isn't verbose — you are":
- No `I`-prefix interfaces, no `AbstractXxxImpl` parallel trees.
- Records for immutable carriers (snapshots, events, DTOs).
- Concrete services, not interface-per-service unless the API module
  actually needs decoupling.
- Method names follow `verb()` not `performXxxOperation()`.
- No Optional-wrapping of returns when the caller null-checks anyway.
- Fluent builders only where JEConfig / JExPlatform already uses them.
- No getter/setter on fields that never escape the package.
- Comments only where the WHY is non-obvious; entity/DTO classes stay
  Javadoc-minimal (Checkstyle allows it for overrides and simple accessors).

## Ready

Awaiting confirmation to begin **Phase 2 — Entities**.
