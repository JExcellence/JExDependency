# Changelog

## Unreleased

### Breaking
- Only the Maven Central mirror and Central are searched by default. Libraries from JitPack, PaperMC, SpigotMC,
  NeetGames, Auxilor, Tcoded or the Gradle plugin portal need their repository in `dependencies.yml`
  (`repositories:` or `coordinate @ url`), `community-repositories: true`, or the server property
  `-Djedependency.repository.community=true`. A failed lookup logs this hint once.
- `TransitiveDependencyResolver(File, List<RepositoryType>)`, added after 2.2.0 and never released, is replaced
  by `TransitiveDependencyResolver(File, PomSource)`.
- Automatic relocation only relocates libraries the plugin's own classes reference under the relocation prefix.
  Before, the Paper loader relocated every library it found except a built-in list (Hibernate, Jackson, JDA,
  OkHttp, Kotlin, Apache and others), which broke plugins that use a library by its original package names. The
  built-in list is gone; only JDK, Jakarta and XML API packages are always excluded. Explicit relocations from
  `-Djedependency.relocations` or `relocations:` in `dependencies.yml` are no longer refused for Hibernate,
  Jackson, JDA or Apache packages.

### Changed
- `JEDependency.initializeWithRemapping` on Spigot plans relocations like the Paper loader, resolves transitive
  dependencies and downloads in parallel.
- The Paper loader resolves transitive dependencies (turn off with `-Djedependency.transitive=false`) and marks its
  classpath record as complete when every library loaded. `JEDependency.initialize` then skips its second pass,
  which also stops relocated and unrelocated copies of the same library from both being loaded.
- Downloads use `java.net.http.HttpClient` with HTTP/2, connection reuse and at most six parallel requests per
  host. Retries honour `Retry-After`, and a redirect from HTTPS to HTTP is refused.
- Jars are verified against the repository's `.sha256` file when it exists, otherwise its `.sha1` file.
- Transitive POMs are fetched one tree level at a time, in parallel, and from the same repositories as the jars.
  The nearest declaration of an artifact now wins, as in Maven; before, the first one found depth-first won.
- Maven Central artifacts are downloaded from a mirror first and from Central only when the mirror cannot serve them.
  Central's terms do not allow using it as a download CDN for end users, and it rate-limits networks that download
  too much. The default mirror is Google's (`maven-central.storage-download.googleapis.com`), the same one Paper
  uses.
- The connectivity check asks the mirror first and counts any HTTP answer as online, so a mirror that answers its
  root URL with 404 is not mistaken for a missing network. When the mirror does not answer but Central does, that
  run downloads from Central directly instead of waiting for mirror timeouts on every artifact.
- The Paper plugin loader and `RemappingDependencyManager` now run the connectivity check too, so they fail fast
  when offline and skip an unreachable mirror.

### Added
- `jedependency.repository.central` system property and support for Paper's `PAPER_DEFAULT_CENTRAL_REPOSITORY`
  environment variable to choose the mirror.
- `RepositoryType.MAVEN_CENTRAL_MIRROR` and `CentralMirror`.
- `DependencyDownloader.activeRepositories()` and a `TransitiveDependencyResolver` constructor that takes the
  repositories to fetch POM files from.
- `SharedCacheManager.isUsableJar(Path)`.
- `RelocationPlanner`; `relocation-prefix:` and `relocations:` in `dependencies.yml`;
  `RemappingDependencyManager.setTransitiveResolutionEnabled(...)`; `LoaderClasspathFile.isComplete(...)`.
- `dependencies.yml`: `repositories:`, `community-repositories:`, `relocation-excludes:`, and per-library
  `@ <url>` and `sha256=<hex>`. Read by the new `DependencyDescriptor`, `DependencyEntry` and
  `YamlDependencyLoader.loadDescriptor(...)`.
- Private repositories: credentials from `JEDEPENDENCY_AUTH_<HOST>` environment variables (`RepositoryCredentials`).
- `DependencyDownloader.configure(...)`, `setCommunityRepositories(...)`, `repositoryUrls(...)`, `fetchPom(...)`;
  `RepositoryType.defaultOrder(...)`, `isCommunity()`, `COMMUNITY_PROPERTY`; `RepositoryUrl`; `PomSource`;
  `DownloadThreads`, which falls back to a daemon thread pool on runtimes without virtual threads.

### Fixed
- A download interrupted while waiting to retry (for example during server shutdown) stops instead of running the
  remaining attempts without waiting.
- Jars from the old per-plugin folder are moved into the shared cache only when they open as a jar. A broken file
  there used to be accepted, and a broken file already in the shared cache could be reported as a successful
  migration.
- Valid jars smaller than 1 KB are no longer rejected.
- `DependencyDownloader.setMaxRetries` is safe to call while downloads run on other threads.

## 2.2.0 (2026-11-17)

### Fixed
- Paper plugin loader: with remapping on (the default), every jar in the server-wide cache, including other
  plugins' libraries, was remapped and put on each plugin's classpath. Only the plugin's own libraries are remapped
  and loaded now. If any of them fails to remap, all are loaded unrelocated instead of the failed jar going
  missing.
- `JEDependency.initialize` on Paper injected every jar in `libraries/remapped/`, including leftovers from earlier
  runs. It now injects exactly the jars the Paper loader recorded in `libraries/paper-loader-classpath.txt`.
- Remapped copies were reused by file date alone. After adding a library, an already remapped jar kept pointing at
  the library's original package and failed with `NoClassDefFoundError`. Copies now live in
  `libraries/remapped/<set>/`, one folder per relocation set, so a changed set is always remapped again.
- A failed remap no longer leaves a partial jar that the next start would reuse; both remap paths now load all
  libraries unrelocated when any one of them fails.
- The Paper loader and the remapping manager now pick up jars from the plugin's old `libraries/` folder again
  instead of downloading them a second time.
- Two plugins in the same server downloading the same library at the same time no longer fail with
  `OverlappingFileLockException`; the second one waits for the first.
- Lock files are no longer deleted after each download, which could let two processes download the same jar at once.
- Downloads no longer contact dead or duplicate repositories. `RepositoryType.MAVEN_CENTRAL` pointed to
  `central.maven.org`, which has been offline since 2020, and now points to `repo1.maven.org`, the host the connectivity check already uses. Sonatype
  OSSRH (shut down in 2025), JCenter (redirects to Maven Central) and the duplicate Maven Central hosts are skipped.
- The library no longer ships a sample `dependency/dependencies.yml` that could collide with your plugin's own file.

### Added
- Cache layout marker (`cache-format.properties`) so future layout changes cannot mix with older versions.
- `RepositoryType.searchOrder()` and `RepositoryType.isRetired()`.
- Standalone Gradle build, JitPack support, CI, Javadoc publishing, security and contribution guides.

### Deprecated
- `RepositoryType` constants `MAVEN_CENTRAL_REPO1`, `APACHE_MAVEN`, `SONATYPE_OSS`, `SONATYPE_OSS_SNAPSHOTS` and
  `JCENTER`. They are never contacted; use `MAVEN_CENTRAL`.

### Changed
- ASM 9.11.1 for remapping, so libraries compiled for newer Java versions can be relocated.
- Compiled against Paper API 1.21.4 instead of a newer API, to keep newer API calls out of the library.

## 2.1.0
- Shared server-wide download cache, retry with backoff, strict checksum handling, offline detection and
  `FailurePolicy`.

## 2.0.0
- Paper plugin loader support, optional ASM relocation, transitive POM resolution.
