# JExDependency

Modern runtime dependency provisioning for Paper and Spigot plugins. JExDependency keeps plugin artifacts lightweight by downloading, relocating, and wiring third-party libraries on demand while honouring the platform's security posture.

## Highlights

- **Pluggable loaders** – Works with the Paper plugin loader handshake or legacy Bukkit bootstrap, automatically detecting the optimal pipeline.
- **Deterministic classpath** – Resolves coordinates from bundled YAML descriptors and optional programmatic overrides, caching artefacts under the plugin data folder.
- **Selective remapping** – Invokes the relocation pipeline only when requested, preserving CPU cycles on hot paths yet guaranteeing isolation when conflicting packages are detected.
- **Java 21 ready** – Handles module de-encapsulation and automatic `--add-opens` semantics so reflective libraries remain functional on modern JVMs.

## Usage pattern

1. **Bundle descriptors** under `dependency/*.yml`. JExDependency merges generic, Paper-only, and Spigot-only files, normalises coordinates, and deduplicates versions.
2. **Call the bootstrapper** inside `onLoad`:
   ```java
   public final class ExamplePlugin extends JavaPlugin {
       @Override
       public void onLoad() {
           JEDependency.initializeWithRemapping(this, ExamplePlugin.class);
       }
   }
   ```
   Use `initializeAsync` when you need non-blocking startup; the returned `CompletableFuture` completes once jars are injected.
3. **Consume dependencies** after the future resolves. Relocated jars live under `plugins/ExamplePlugin/libraries/remapped` while non-remapped artefacts remain in `libraries`.

## Configuration reference

| Property | Description |
| --- | --- |
| `-Djedependency.remap` | `auto` by default; force `true` or `false` to control relocation. |
| `-Djedependency.relocations` | Comma-separated list of `pattern=target` overrides. |
| `-Djedependency.relocations.prefix` | Global prefix applied to automatically relocated packages. |
| `-Djedependency.relocations.excludes` | Packages that should never be relocated. |

All properties are read during bootstrap and mirrored by the Paper loader so console output stays consistent across entrypoints.

## Logging and observability

- Progress messages are emitted through `CentralLogger` with plugin-qualified logger names. Enable FINE logs during development to inspect download progress and relocation summaries.
- Failures include sanitized coordinates and the destination path; PII such as server file system roots is redacted.
- Each bootstrap cycle records start/end timestamps and total dependency counts, enabling downstream automation to detect divergence across shards.

## Security practices

- Checksums from Maven repositories are validated before JARs are cached; corrupted downloads trigger retries and fall back to clean directories.
- Remapping runs inside a sandboxed `URLClassLoader` to prevent partially relocated classes from leaking into the main plugin loader on failure.
- Temporary directories are wiped on success and failure, eliminating stale bytecode that could be hijacked between restarts.

## Further reading

- [`JEDependency`](src/main/java/de/jexcellence/dependency/JEDependency.java) – Bootstrap entrypoints and configuration handling.
- [`DependencyManager`](src/main/java/de/jexcellence/dependency/manager/DependencyManager.java) – Core resolution pipeline.
- [`RemappingDependencyManager`](src/main/java/de/jexcellence/dependency/manager/RemappingDependencyManager.java) – Relocation-aware manager used when remapping is enabled.
