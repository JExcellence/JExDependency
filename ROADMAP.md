# Roadmap

What is planned for JExDependency, in priority order. Each item names the problem it solves and how we will know
it is done. Items marked **help wanted** are good places to contribute; open an issue or discussion before starting
larger ones.

Status: `planned`, `in progress`, `done`. Last reviewed 2026-11-17.

## P0: correctness and fair use (next minor release)

| # | Item | Why | Done when |
|---|---|---|---|
| 1 | **Download Maven Central artifacts from a mirror by default** | Maven Central's terms do not allow using it as a CDN for end users, and Central rate-limits whole networks that download too much. Paper switched its own loader to Google's Central mirror in 2025 for this reason. | Default order is `maven-central.storage-download.googleapis.com`, then Central. `PAPER_DEFAULT_CENTRAL_REPOSITORY` and a `jedependency.repository.central` property override it. |
| 2 | **Community repositories become opt-in** | Today an artifact missing from Central is fetched from the first of seven community repositories that has it. That is slow (a chain of 404s) and lets a lookalike artifact in any of them win. | `dependencies.yml` accepts a `repositories:` list and an optional `repository:` per dependency; only Central (mirror) is built in. The old behaviour stays behind a flag for one major version. |
| 3 | **Automatic relocation on Spigot** | Only the Paper loader detects packages to relocate. On Spigot, `initializeWithRemapping` relocates nothing unless the server sets `-Djedependency.relocations`. | Both paths share one relocation planner; the same YAML produces the same relocated jars on Spigot and Paper. |
| 4 | **Transitive dependencies in the Paper loader** | The Paper loader loads only the declared jars; transitive ones are resolved later by the plugin side, so Paper plugins resolve twice and classes needed during bootstrap can be missing. | The loader resolves the full tree once; the plugin side reuses the recorded result instead of resolving again. |
| 5 | **Suite-specific relocation excludes move to configuration** | The automatic relocation skips a hard-coded list (Hibernate, JDA, OkHttp, Kotlin and more) that came from one project's needs. | Defaults cover only JDK and Jakarta packages; projects list their own excludes in `dependencies.yml`. |
| 6 | **Real-server test matrix** | CI proves the code compiles and unit tests pass, not that it loads on a server. | CI boots a test plugin on Paper and Spigot (1.20.4, 1.21.x, latest) and Folia, and checks that a library class loads. |

## P1: speed and non-blocking startup

Cold start today: POMs are fetched one at a time, and the Spigot remapping path downloads jars one after another.
Every item below is measured with a startup report (item 13) before and after.

| # | Item | Expected effect |
|---|---|---|
| 7 | **Lockfile generated at build time** by a Gradle plugin: the full dependency tree with URLs and SHA-256 hashes in `dependencies.lock`. | No POM requests at runtime at all; the server downloads exactly the tested jars and verifies them. Largest single speed and security gain. **help wanted** |
| 8 | **Parallel POM resolution** when no lockfile exists: each level of the dependency tree is fetched concurrently with a per-host limit. | Cold resolution time grows with tree depth instead of tree size. |
| 9 | **Parallel downloads on the Spigot remapping path**, like the other paths already do. | Cold start with remapping no longer scales linearly with library count. |
| 10 | **`java.net.http.HttpClient`** with HTTP/2, connection reuse and a bounded number of requests per host. | Fewer TLS handshakes; friendlier to mirrors. |
| 11 | **Parallel remapping and a server-wide remap cache** keyed by jar hash and relocation set. | Two plugins with the same libraries and settings remap once per server, not once each. |
| 12 | **One startup handshake on Paper**: when the loader's record is complete, the plugin side skips download and resolution. | Paper plugins do the work once. |
| 13 | **Startup report**: one log line with resolve, download, verify, remap and inject times and cache hits. | Server owners can see what startup costs and whether the cache works. **help wanted** |
| 14 | **Typed async API**: `initializeAsync` returns a `CompletionStage<LoadResult>` with what was loaded and what failed, plus a helper that continues on the right thread on Bukkit and on Folia's global region scheduler. | Plugins can keep heavy setup off the main thread without guessing. |

## P2: features

| # | Item |
|---|---|
| 15 | **Isolated class loader mode**: load a plugin's libraries into a child class loader instead of relocating them. Faster than remapping and keeps original package names. |
| 16 | **SHA-256 and SHA-512 checksums**, preferred over SHA-1 when the repository publishes them; optional pinned hashes in YAML. **help wanted** |
| 17 | **Private repositories** with credentials read from environment variables, never from the plugin jar. |
| 18 | **Pre-fill the cache offline**: a small command-line tool that downloads everything a set of plugins needs, for Docker images and networks without internet access. |
| 19 | **Cache cleanup command** that removes unused relocation-set folders and jars no plugin references. **help wanted** |
| 20 | **Java 16 baseline** (Minecraft 1.17 and newer): only the virtual-thread executor needs a fallback. |
| 21 | **Velocity and BungeeCord adapters**, if there is demand. |

## Code quality gates

These keep the library dependable as it grows. They run in CI on every pull request.

| Gate | Purpose |
|---|---|
| JSpecify `@NullMarked` packages checked by NullAway (Error Prone) | Null contracts the compiler enforces, replacing JetBrains annotations. |
| Spotless formatting | No style discussions in reviews. |
| JaCoCo with a minimum line and branch coverage on `cache`, `downloader`, `resolver`, `remapper` | Logic that touches files and the network stays tested. |
| japicmp against the last release | No accidental breaking change to the public API within a major version. |
| Sealed result types (`DownloadResult` as `Success` or `Failure`), records for value types, no reflection between the library's own classes | Smaller, clearer code paths. |
| Reproducible builds, CycloneDX SBOM, signed artifacts, Maven Central publishing | Users can verify what they ship. |

## Done

- 2.2.0 (merged, release pending): standalone build, shared cache layout marker and concurrent-download fix, dead repositories removed from the
  search, Paper loader loads only the plugin's own jars, remapped copies keyed by relocation set.
