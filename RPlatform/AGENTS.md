# RPlatform Contribution Guide

## Lifecycle Expectations
- Call `RPlatform#initialize()` early in your plugin's `onEnable`. The method delegates to the scheduler returned by `ISchedulerAdapter#create(...)`, so Folia servers use the Folia implementation while Paper/Spigot fall back to the Bukkit adapter. The initialize routine must finish before you register commands or services that depend on the translation manager, command updater, or database layer created inside the async block.
- Any metrics (`initializeMetrics(int serviceId)`) or placeholder (`initializePlaceholders(String identifier)`) bootstrapping happens **after** the main initialize future completes. This ensures the async setup has populated the translation manager, command updater, and database resources that downstream plugins expect.

## Scheduler Delegation
- `ISchedulerAdapter` selects its implementation via reflection: Folia tries `scheduler.impl.FoliaISchedulerImpl`, otherwise `scheduler.impl.BukkitISchedulerImpl`. If you add a new scheduler target, provide a matching impl class and keep the reflective creation path aligned. Downstream code should schedule work via the adapter rather than direct Bukkit/Folia APIs.

## Database Resources & Premium Detection
- `initializeDatabaseResources()` creates `<plugin data folder>/database/hibernate.properties` by copying the bundled `database/hibernate.properties` resource, then builds the shared `EntityManagerFactory` through `JEHibernate`. Keep the properties file in sync with any schema changes and verify `JEHibernate` still resolves properly when modifying persistence code.
- Premium detection relies on `detectPremiumVersion(Class<?> resourceClass, String resourcePath)` finding a marker resource on the class loader. When shipping premium-only features, ensure the resource path is bundled so this flag can be flipped reliably.

## Documentation Alignment
- Whenever you change workload executors, placeholder APIs, statistics, or metrics behaviour, mirror the updates in `README.md`, `PLATFORM_GUIDE.md`, and any example snippets. The README's "Key Features" section highlights workload management, placeholders, statistics, and metrics—keep those examples accurate alongside code changes.
