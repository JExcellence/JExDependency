# RCore Agent Guidelines

> **Note:** Read the root `AGENTS.md` for repository-wide workflow, commit, and testing guidelines before following the module-specific notes below.

## Service Implementation Practices
- Prefer `CompletableFuture`-driven flows for asynchronous work. Chain stages with explicit executor arguments to avoid relying on the common fork-join pool when interacting with Bukkit schedulers or shared thread pools.
- Use the service-specific `Executor` provided by the module (e.g., `ServiceExecutionContext#getExecutor`) for all asynchronous callbacks to maintain consistent threading guarantees across RCore and consuming modules.
- Guard every externally supplied dependency, identifier, or payload with `Objects.requireNonNull(...)` at the top of public entry points to prevent latent `NullPointerException`s during runtime boot.

## Aggregate Construction and Persistence
- Compose full entity aggregates (such as `RPlayer` with its statistics, inventory snapshots, and metadata) prior to invoking persistence repositories. Ensure aggregates are internally consistent before storage to minimize partial writes.
- Leverage `CentralLogger` to log aggregate lifecycle milestones (`create`, `load`, `save`, `delete`). Include contextual identifiers (UUIDs, profile names) so RDQ and other consumers can correlate events.

## Cross-Module Compatibility
- Treat public APIs exposed from `rcore-common` as stable contracts for RDQ, RPlatform, and external plugins. Changes must maintain backward compatibility or provide adapters marked with clear deprecation timelines.
- Coordinate protocol or schema updates with RDQ maintainers and document version gates in the corresponding module before merging.
