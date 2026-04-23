# JExQuests Refactor Plan — greenfield replacement for RDQ

## Context

`JExQuests` replaces the legacy `RDQ` plugin (now archived as `RDQ_old`,
not in the build graph). The legacy codebase was 440 Java files with
168 Lombok hits, `--enable-preview`, `rplatform`, `CentralLogger`, old
JEHibernate `CachedRepository`, 24 locale YAMLs, and a bloated schema
of 39 entities (many of which are redundant — separate entities for
every reward/requirement/category relationship).

The new codebase is a **clean rewrite** mirroring the JExCore /
JExEconomy shape: 5 modules (api, common, free, premium), JExPlatform,
JEHibernate 3.0.2 thin, JExCommand 2.0, R18nManager, InventoryFramework
with AnvilInputFeature, Hibernate 7.1.4. `RDQ_old` is available
read-only at `RDQ_old/` as a reference for gameplay semantics.

Phase 1 (scaffold + en_US.yml) is already complete and green.

## Slim schema (13 entities, not 39)

Rationale: legacy RDQ modelled every reward/requirement/category
relationship as its own entity. That's overkill for data that's
loaded from YAMLs at startup and mutated rarely. The clean design
stores reward and requirement configurations as **JSON blobs** on the
parent entity, parsed in the service layer to sealed-interface
domain types.

Naming drops the `R`/`RDQ` prefix (JExCore precedent: `CorePlayer`
not `RCorePlayer`). The JExQuests per-player row is `QuestsPlayer` —
references `JExCore.CorePlayer` by UUID but keeps its own progression
state.

### Quest system (5 entities)

| Entity | Purpose |
|---|---|
| `Quest` | Quest definition: identifier, category, icon JSON, difficulty, repeatable/maxCompletions/cooldown/timeLimit, enabled, requirement+reward JSON, prerequisites/dependents |
| `QuestTask` | One task within a quest: FK quest, task identifier, icon JSON, orderIndex, difficulty, sequential flag, requirement+reward JSON |
| `QuestsPlayer` | Per-player row: UUID (FK-style to `CorePlayer`), perkSidebar toggle, scoreboard toggle, createdAt |
| `PlayerQuestProgress` | Progress row per (player, quest): status enum, currentStepIndex, startedAt, completedAt, completionCount |
| `PlayerTaskProgress` | Progress row per (player, quest, task): numerator/denominator, updatedAt |

### Rank system (3 entities)

| Entity | Purpose |
|---|---|
| `RankTree` | Identifier, displayName, ordering |
| `Rank` | FK tree, identifier, order, displayName, requirement+reward JSON |
| `PlayerRank` | Per-player: FK player, treeIdentifier, currentRankIdentifier, promotedAt, progressionPercent |

### Bounty system (2 entities)

| Entity | Purpose |
|---|---|
| `Bounty` | targetUuid, issuerUuid, amount, currency, placedAt, expiresAt, status enum |
| `BountyClaim` | FK bounty, killerUuid, claimedAt, payoutAmount |

### Perk system (2 entities)

| Entity | Purpose |
|---|---|
| `Perk` | Identifier, category, perkType enum, displayName, description, icon JSON, requirement+reward JSON, cooldownSec |
| `PlayerPerk` | FK player, perkIdentifier, enabled, unlockedAt, lastActivatedAt |

### Machine system (1 entity)

| Entity | Purpose |
|---|---|
| `Machine` | ownerUuid, machineType, world+x+y+z+facing, storage JSON, upgrade JSON, trusted players JSON, createdAt, lastActiveAt |

Upgrade levels, trust entries, and storage items fold into JSON blobs.
Separate tables would be premature — nothing queries them relationally.

**Total: 13 entities.**

## Phase list (greenfield port)

| # | Theme | Files | Status |
|---|---|---|---|
| 1 | Scaffold + config + en_US.yml | ~15 | done |
| 2a | Quest system entities (5) + enums | 8 | next |
| 2b | Rank + Bounty entities (5) + enums | 7 | |
| 2c | Perk + Machine entities (3) + enums | 5 | |
| 3 | Repositories (13 `AbstractCrudRepository<T, Long>`) | 14 | |
| 4 | Services (6-8 concrete services on `ServicesManager`) | 10 | |
| 5 | YAML loaders (quests/ranks/perks/machines) | ~10 | |
| 6a | Quest commands + handlers | ~8 | |
| 6b | Rank + Bounty commands + handlers | ~8 | |
| 6c | Perk + Machine commands + handlers | ~8 | |
| 7a | Quest views (overview, detail, category, history) | ~8 | |
| 7b | Rank views (hierarchy, grid, detail) | ~6 | |
| 7c | Bounty + Perk views | ~8 | |
| 7d | Machine views (controller, storage, upgrade) | ~6 | |
| 8 | Listeners (join/quit, block-break, entity-death, etc.) | ~6 | |
| 9 | `jexquests-api` public SPI (providers + events + snapshots) | ~12 | |
| 10 | JExCore statistics bridge + JExEconomy economy bridge | 4 | |
| 11 | Sidebar + PlaceholderAPI expansion | 4 | |
| 12 | Free / Premium delegates — perk + machine installers | ~6 | |
| 13 | Wiring (JExQuests orchestrator — lifecycle pipeline) | 1 | |
| 14 | Tests (value objects, progression, reward parser) | ~10 | |
| 15 | Final verification | — | |

**Target: ~150 Java files, not 440.** Anything extra will prompt a
scope review.

## Conventions (non-negotiable)

- Java 21 toolchain, `options.release.set(21)` (JExCore standard)
- No Lombok
- No `--enable-preview`
- No `rplatform`, `CentralLogger`, `R18nBuilder`
- Entities extend `LongIdEntity`, pure data, no behaviour
- Repositories extend `AbstractCrudRepository<T, Long>` with
  async `query().and().firstAsync()`
- Services concrete, registered on `Bukkit.getServicesManager()`,
  unregistered in `onDisable`
- Every `CompletableFuture` chain ends in `.exceptionally(ex -> {
  log; return fallback; })`
- Every user-visible string through `R18nManager` via the keys in
  `translations/en_US.yml`
- Commands = JExCommand 2.0 YAML trees + handler classes with
  `handlerMap()`
- Views = InventoryFramework, 3-line lore max (description → blank →
  cyan action prompt)

## Ready

Phase 2a next: Quest system entities.
