# RDQ

RaindropQuests – the gameplay layer that powers perks, quests, bounties, and ranks across RaindropCentral deployments.

## Architecture

- **Layered services** – Commands invoke views, which delegate to services backed by repositories. Each layer enforces async/sync boundaries through the RDQ executor.
- **Perk runtime** – `DefaultPerkRegistry` adapts `RPerk` entities into `PerkRuntime` instances with centralized cooldown/state tracking.
- **Event handling** – `DefaultPerkTriggerService` filters Bukkit events, ensures cooldown eligibility, and emits audit records for every trigger attempt.
- **Internationalisation** – All player-facing messages flow through JExTranslate with MiniMessage support.

## Key services

| Service | Responsibility |
| --- | --- |
| `PerkManager` | Loads runtimes, registers listeners, manages cooldown/state services, and exposes cleanup hooks. |
| `PerkAuditService` | Emits hashed-fingerprint JSON logs capturing activation, deactivation, triggers, expiries, and cleanup flows. |
| `PerkRuntimeStateService` | Maintains activation windows and concurrent-user limits per perk. |
| `CooldownService` | Stores cooldown expiries in memory with player-level purge operations. |

## Observability & safety

- Perk audit entries are nested JSON payloads with sanitized keys and truncated values to prevent log injection while preserving forensic utility. ([PerkAuditService.java](RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/perk/runtime/PerkAuditService.java#L32-L212))
- `DefaultPerkRegistry` applies sliding-window log throttling so repeated failures cannot flood log files, and metadata drawn from configuration is sanitized before being persisted or logged. ([DefaultPerkRegistry.java](RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/perk/runtime/DefaultPerkRegistry.java#L45-L370))
- Trigger exceptions are reported once per throttle window with hashed player fingerprints, striking a balance between incident visibility and noise reduction. ([DefaultPerkTriggerService.java](RDQ/rdq-common/src/main/java/com/raindropcentral/rdq/perk/runtime/DefaultPerkTriggerService.java#L17-L120))

## Lifecycle

1. `PerkInitializationManager` prepares perk types, registries, state services, cooldown tracking, and the perk event bus during RDQ startup.
2. `DefaultPerkManager.initialize()` reloads database-defined perks, registers listeners, and announces runtime counts via `CentralLogger`.
3. Player disconnect hooks call `PerkStateService.cleanupPlayerState(UUID)` to flush runtime caches and cooldowns.
4. Shutdown clears listeners, runtime state, cooldowns, and flushes audit logs.

## Developer checklist

- Always sanitize metadata coming from configuration/DB before feeding it into perk runtimes.
- Hash player UUIDs or use the audit service when emitting log statements.
- Wrap expensive DB operations in `CompletableFuture` via the RDQ executor to keep the main thread responsive.
