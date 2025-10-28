# RDQ Module Contributor Notes

> **Note:** Read the root `AGENTS.md` for repository-wide workflow, commit, and testing guidelines before following the module-specific notes below.

## Lifecycle Overview
- **Staged enable pipeline**
  1. **Platform initialization** occurs before any gameplay components spin up. Use this stage to resolve edition-specific services and bind shared platform primitives.
  2. **Component and view setup** follows, creating views, registering GUI frames, and wiring listeners. Keep constructor logic lightweight; defer heavy lifting to asynchronous tasks where possible.
  3. **Repository wiring** is the final step. Create repositories only after the platform and view frames are ready so data access aligns with the initialized UI components.
- Respect the `runSync` boundary. Only call synchronous Bukkit/Paper APIs or mutate shared state inside the provided `runSync` block. Perform blocking IO or expensive computation on background executors and re-enter the sync context through `runSync` when touching the main thread.

## Execution Model
- Prefer the virtual-thread executor for asynchronous work. It provides lightweight concurrency suitable for high fan-out operations.
- When the runtime cannot supply virtual threads, fall back to the fixed-thread executor exposed by the module. Ensure tasks remain responsive under both modes.
- Implement `onDisable` hooks to shut down executors, cancel scheduled tasks, flush buffered state, and close repositories. Leaving background jobs running after disable is considered a bug.

## Logging & Telemetry
- Leverage `PerkAuditService` for lifecycle tracking and ensure custom context keys are sanitized to `[A-Za-z0-9._-]`.
- When recording diagnostics, hash or alias player identifiers and throttle exception-heavy code paths to prevent log spam.
- Store rich context inside nested maps (for example `{ "context": { ... } }`) instead of interpolating attacker-controlled strings directly into message templates.

## Integration Touchpoints
- Commands must be registered through `CommandFactory`. Extend or add commands by supplying new factories instead of manual registrations so that edition-specific command trees remain intact.
- UI flows should compose `ViewFrame` instances or derivatives, ensuring navigation and edition gating logic stay consistent. When introducing new views, follow the existing frame registration order established during component setup.
- Repositories are constructed during the wiring stage using factory helpers. Add new repositories by extending the existing constructors instead of bypassing them; this guarantees free/premium editions receive the correct data sources.

## Contribution Tips
- Align feature work with the staged lifecycle. Each change should identify its lifecycle stage and thread context.
- Provide unit or integration coverage whenever modifying executors, lifecycle hooks, or repositories.
- Document any new runSync boundaries or executor configurations directly within the affected classes for future contributors.
