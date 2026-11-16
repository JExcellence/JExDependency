# Changelog

## 2.2.0 (unreleased)

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
- Compiled against Paper API 1.21.4 instead of a newer API, to keep newer API calls out of the library.

## 2.1.0
- Shared server-wide download cache, retry with backoff, strict checksum handling, offline detection and
  `FailurePolicy`.

## 2.0.0
- Paper plugin loader support, optional ASM relocation, transitive POM resolution.
