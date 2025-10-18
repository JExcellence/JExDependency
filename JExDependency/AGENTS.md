# JExDependency Contributor Notes

> **Note:** Read the root `AGENTS.md` for repository-wide workflow, commit, and testing guidelines before following the module-specific notes below.

## Initialization entrypoints
- `JEDependency.initialize(plugin, anchorClass[, additionalDependencies])` performs synchronous dependency loading through `DependencyManager`. Use this for the common Paper/Spigot flow when your plugin can block during `onLoad`/`onEnable` and you do **not** need remapping. Internally it collects YAML + optional extras, downloads the jars into `<data>/libraries`, de-encapsulates modules, and injects them before returning.
- `JEDependency.initializeAsync(plugin, anchorClass[, additionalDependencies])` delegates to `DependencyManager.initializeAsync`. Choose this when startup needs to continue without waiting for downloads; you are responsible for awaiting the returned `CompletableFuture` before touching classes supplied by runtime dependencies.
- `JEDependency.initializeWithRemapping(plugin, anchorClass[, additionalDependencies])` forces the remapping pipeline when the `RemappingDependencyManager` is on the classpath. Use it if you require relocations regardless of the `-Djedependency.remap` system property. It falls back to the standard manager with warnings if remapping cannot start.

## Configuration inputs
- Dependency coordinates are sourced from any `dependency/*.yml` bundled alongside the anchor class, plus any strings supplied through the optional `additionalDependencies` array.
- JVM properties control behavior at runtime:
  - `-Djedependency.remap` (`auto` default) toggles package remapping for both the Paper loader and in-plugin bootstrap.
  - `-Djedependency.relocations`, `-Djedependency.relocations.prefix`, and `-Djedependency.relocations.excludes` configure explicit or automatic relocation rules.
  - `-Dpaper.plugin.loader.active` is set internally when the Paper loader is running so the main API continues bootstrap logging and classpath injection.
- When remapping succeeds, jars land under `<data>/libraries/remapped`; otherwise `<data>/libraries` remains the active folder. Paper’s loader chooses the effective directory the same way so keep any new code consistent with those semantics.

## Paper loader alignment
- The Paper integration preloads dependencies inside `PaperPluginLoader.classloader`. It mirrors the same YAML lookup, download pipeline, remapping mode detection, relocation helpers, and logging messages used by `JEDependency`. When editing bootstrap behavior, update both the loader and `JEDependency` paths together so runtime logging (“Initializing plugin classpath…”, remapping warnings, injected counts) and injection order stay aligned.
- The loader sets `paper.plugin.loader.active=true`, seeds `<data>/libraries` before the plugin’s `onLoad`, and then the plugin-side `initialize*` call injects those jars into the running classloader (preferring `libraries/remapped` if present). Preserve this handshake in future refactors.
