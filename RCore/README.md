# RCore

Shared services and infrastructure for the RaindropCentral plugin ecosystem. RCore exposes persistence, encryption, scheduler adapters, and telemetry primitives consumed by RDQ, RPlatform, and edition-specific modules.

## Capabilities

- **Database bootstrap** – Centralises `EntityManagerFactory` creation, schema migrations, and repository registration for player statistics, perks, and quests.
- **Service executors** – Provides tuned `ExecutorService` instances (virtual-thread preferred) for asynchronous operations, falling back to fixed pools when virtual threads are unavailable.
- **Integration gateways** – Hosts APIs that other plugins can depend on (e.g., statistics service, perk hooks) with strict backwards compatibility expectations.
- **Audit logging** – Routes lifecycle events through `CentralLogger`, attaching correlation identifiers for cross-module diagnostics.

## Startup sequence

1. `RCoreFree`/`RCorePremium` delegates to `RPlatform` to initialise logging, configuration folders, metrics, and dependency injection.
2. Repositories are constructed from the platform-managed `EntityManagerFactory` and exposed via service getters.
3. Commands and view frames are registered, respecting edition-specific feature flags.
4. Shutdown closes repositories, stops executors, and flushes `CentralLogger` to guarantee durable logs.

## Observability & security

- Sensitive identifiers such as player UUIDs are hashed before being logged; cross-module log correlation relies on these fingerprints rather than raw IDs.
- Repository mutations (create/update/delete) log success and failure outcomes with structured context payloads so administrators can trace anomalies.
- Configuration values loaded from disk are validated with Jakarta Validation; failures prevent boot to avoid running with malformed secrets.

## Related modules

- [`rcore-common`](rcore-common/) – Shared classes used by both free and premium editions.
- [`rcore-free`](rcore-free/) – Lightweight edition wiring, including mock data providers.
- [`rcore-premium`](rcore-premium/) – Premium-only integrations (external databases, placeholder bridges, advanced metrics).
