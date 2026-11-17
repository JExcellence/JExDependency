<div align="center">

# JExDependency

**Runtime dependency loading for Minecraft plugins on Spigot, Paper and Folia.**
Stop shading Hibernate, Jackson or JDBC drivers into your plugin jar. Declare them, and the server downloads,
verifies, optionally relocates and loads them at startup.

[![Build](https://github.com/JExcellence/JExDependency/actions/workflows/ci.yml/badge.svg)](https://github.com/JExcellence/JExDependency/actions/workflows/ci.yml)
[![JitPack](https://jitpack.io/v/JExcellence/JExDependency.svg)](https://jitpack.io/#JExcellence/JExDependency)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 21+](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://adoptium.net/)

[Quick start](#quick-start) · [How it works](#how-it-works) · [Paper plugins](#paper-plugins) · [Shading guide](#shading-jexdependency-into-your-plugin) · [Configuration](#configuration) · [Troubleshooting](#troubleshooting) · [Roadmap](ROADMAP.md)

</div>

---

## Is this for you?

You probably want JExDependency if one of these sounds familiar:

- Your plugin jar is 15 MB because it shades a database driver, an ORM or a JSON library.
- Another plugin updated Guava, Gson or Caffeine and yours now fails with `NoSuchMethodError` or
  `NoClassDefFoundError`.
- You use Spigot's `libraries:` block but need a library that is not on Maven Central, or you need to relocate it.
- You maintain several plugins on one network and every one of them downloads and stores the same libraries.

What you get:

- **Small plugin jars.** Only JExDependency (about 120 KB) and ASM are inside your jar; everything else is
  downloaded once per server.
- **One shared cache per server.** Plugins using JExDependency share downloads. Parallel startups never download the
  same file twice, and a partially written file is never used.
- **Transitive resolution.** Dependencies declared in each library's POM are resolved for you.
- **Optional relocation.** Rename library packages at runtime, the same result as Shadow's `relocate`, so your
  copy of a library cannot clash with another plugin's copy.
- **Paper plugin loader support.** On Paper, libraries can be on the classpath before your plugin class loads.
- **Failure policy.** Decide whether a missing library stops the plugin, is skipped, or goes to your callback.

## Quick start

**1. Add the library** (built by [JitPack](https://jitpack.io/#JExcellence/JExDependency); newer versions are listed under
[releases](https://github.com/JExcellence/JExDependency/releases)):

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.JExcellence:JExDependency:2.2.0")
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.2.2") // downloaded at runtime, not shaded
}
```

<details>
<summary>Maven</summary>

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.JExcellence</groupId>
    <artifactId>JExDependency</artifactId>
    <version>2.2.0</version>
</dependency>
```

</details>

**2. List your runtime libraries** in `src/main/resources/dependency/dependencies.yml`:

```yaml
dependencies:
  - "com.github.ben-manes.caffeine:caffeine:3.2.2"
  - "com.fasterxml.jackson.core:jackson-databind:2.18.2"
  - "com.mysql:mysql-connector-j:9.2.0"
```

**3. Load them in `onLoad()`:**

```java
public final class ExamplePlugin extends JavaPlugin {
    @Override
    public void onLoad() {
        JEDependency.initialize(this, ExamplePlugin.class);
    }
}
```

The libraries are on your plugin's classpath when `onEnable()` runs. Shade and relocate JExDependency itself as
shown in the [shading guide](#shading-jexdependency-into-your-plugin).

## How it works

```mermaid
flowchart TD
    A[Server starts your plugin] --> B{paper-plugin.yml with<br/>JExDependency loader?}
    B -- yes --> C[Paper loader: read dependencies.yml,<br/>download, relocate, add to classpath,<br/>record the list]
    B -- no --> D[onLoad: JEDependency.initialize]
    C --> D
    D --> E[Resolve transitive dependencies from POMs]
    E --> F{In the shared cache?}
    F -- yes --> H
    F -- no --> G[Download with per-artifact lock,<br/>retry with backoff, verify SHA-1]
    G --> H[Optional relocation into<br/>libraries/remapped/set]
    H --> I[Inject into the plugin class loader]
    I --> J[onEnable: libraries available]
```

| Entry point | Blocks `onLoad` | Use it when |
|---|:---:|---|
| `JEDependency.initialize(plugin, anchor)` | yes | Default. |
| `JEDependency.initializeWithRemapping(plugin, anchor)` | yes | You want relocated libraries (see [relocation](#relocation)). |
| `JEDependency.initializeAsync(plugin, anchor)` | no | Startup should continue; wait for the returned `CompletableFuture<Void>` before touching the libraries. |

Each method also accepts a `String[]` of extra coordinates (`group:artifact:version[:classifier]`).
Libraries only one platform needs go into `dependency/paper/dependencies.yml` or
`dependency/spigot/dependencies.yml`; they are merged with the main list.

## Compatibility

| | Status |
|---|---|
| Java | 21 or newer. CI builds and tests on Java 21 and 25. |
| Paper | Used in production on Paper 26.x. |
| Spigot, Folia | Supported by design. Automated tests on real servers are [planned](ROADMAP.md). |
| Libraries you load | Must run on the server's Java version: a Java 25 library will not load on a Java 21 server. |

## Paper plugins

With a `paper-plugin.yml`, Paper runs the JExDependency loader before your plugin class is loaded:

```yaml
name: ExamplePlugin
version: 1.0.0
main: com.example.ExamplePlugin
api-version: "1.21"
loader: de.jexcellence.dependency.loader.PaperPluginLoader
```

If you relocate JExDependency, use the relocated class name, for example
`com.example.libs.jexdependency.loader.PaperPluginLoader`. Still call `JEDependency.initialize(...)` in `onLoad()`:
it injects exactly the jars the loader recorded and resolves anything the loader did not (the loader currently
loads declared libraries only, not their transitive dependencies; see the [roadmap](ROADMAP.md)).

## Relocation

Relocation renames a library's packages (`com.google.gson` becomes `de.jexcellence.remapped.com.google.gson`) so
your copy cannot collide with another plugin's copy.

- **Paper loader:** relocation is on by default (`jedependency.remap=auto`). Packages are detected from your
  libraries automatically and placed under `jedependency.relocations.prefix` (default `de.jexcellence.remapped`).
- **Spigot and `plugin.yml` plugins:** `initializeWithRemapping` relocates only what the server lists in
  `-Djedependency.relocations`. Automatic detection on Spigot is [roadmap item 3](ROADMAP.md).

Relocated copies are stored per relocation set in `plugins/<YourPlugin>/libraries/remapped/<set>/`, so changing
your libraries or the relocation settings always produces fresh copies.

## Shading JExDependency into your plugin

Relocate JExDependency and ASM into your own package. On Spigot, plugin class loaders can see each other's classes,
so two plugins that ship different JExDependency versions under the same package can load the wrong one.

```kotlin
plugins {
    id("com.gradleup.shadow") version "9.4.0"
}

tasks.shadowJar {
    relocate("de.jexcellence.dependency", "com.example.libs.jexdependency")
    relocate("org.objectweb.asm", "com.example.libs.asm")
}
```

**Moving away from shading libraries:** change each shaded library from `implementation` to `compileOnly`, list it
in `dependency/dependencies.yml`, delete its `relocate(...)` line, and keep only the two relocations above.

## Configuration

Server owners can change behaviour with JVM system properties, without rebuilding any plugin.

| Property | Default | Effect |
|---|---|---|
| `jedependency.remap` | `auto` in the Paper loader, otherwise off | `true`, `1`, `yes` or `on` turns relocation on; `false` turns it off. |
| `jedependency.relocations` | none | Explicit relocations: `com.google.gson=>my.libs.gson,org.foo=>my.libs.foo`. |
| `jedependency.relocations.prefix` | `de.jexcellence.remapped` | Package prefix for automatic relocation. |
| `jedependency.relocations.excludes` | none | Comma-separated packages that are never relocated. |
| `jedependency.repository.central` | Google's Central mirror | Maven Central mirror to download from, for example your own Nexus. Falls back to the `PAPER_DEFAULT_CENTRAL_REPOSITORY` environment variable. Set it to `https://repo1.maven.org/maven2` to download from Central directly. Read once at startup. |
| `jedependency.cache.dir` | `<server>/.jexdependency/cache` | Location of the shared cache, for example a Docker volume. |
| `jedependency.path.default` | `/dependency/dependencies.yml` | Resource path of the main dependency list. |
| `jedependency.path.paper` | `/dependency/paper/dependencies.yml` | Resource path of the Paper-only list. |
| `jedependency.path.spigot` | `/dependency/spigot/dependencies.yml` | Resource path of the Spigot-only list. |

Example: `java -Djedependency.cache.dir=/data/jexdependency -jar paper.jar`

### Failure policy

```java
new DependencyManager(this, ExamplePlugin.class)
        .setFailurePolicy(FailurePolicy.DEGRADED)
        .initialize(null);
```

| Policy | When a library cannot be loaded |
|---|---|
| `STRICT` | Startup fails with an exception. |
| `DEGRADED` | The library is skipped and logged; the plugin continues. |
| `CALLBACK` | Your `FailureCallback` decides. |

## Where files go

```
<server>/.jexdependency/cache/                  shared by all plugins
  cache-format.properties                       cache layout version
  jars/<group path>/<artifact>/<version>/...    downloaded libraries
  poms/...                                      POMs for transitive resolution
  locks/                                        one empty lock file per artifact
plugins/<YourPlugin>/libraries/remapped/<set>/  relocated copies, one folder per relocation set
plugins/<YourPlugin>/libraries/paper-loader-classpath.txt   jars the Paper loader added
```

While the server is stopped you can delete the whole cache (it is downloaded again) and any folder in
`libraries/remapped/` (relocation sets that are no longer used are never loaded). Jars in the old per-plugin
`plugins/<YourPlugin>/libraries/` folder are copied into the shared cache automatically.

## Network and security

JExDependency downloads code that runs with your plugin's rights. Here is exactly what it does:

- **When:** only at startup, only for jars and POMs that are not cached yet.
- **Where from:** a Maven Central mirror first (Google's, the same default Paper uses), then Maven Central itself,
  then NeetGames Nexus, JitPack, Auxilor, Tcoded, PaperMC, the SpigotMC snapshot repository and the Gradle plugin
  portal, all over HTTPS. The first repository that has the artifact wins. Maven Central's terms do not allow using
  it as a download CDN, which is why the mirror comes first; set `jedependency.repository.central` or Paper's
  `PAPER_DEFAULT_CENTRAL_REPOSITORY` to use your own mirror. Declaring your own repositories and making community
  repositories opt-in is [roadmap item 2](ROADMAP.md).
- **Verification:** the published `.sha1` is compared with each download and a mismatch rejects the jar. A
  repository without checksums is trusted as is.
- **Versions:** pin exact versions; version ranges are not resolved.

Report vulnerabilities privately as described in [SECURITY.md](SECURITY.md).

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `UnsupportedClassVersionError` | A library needs a newer Java than the server runs. Use an older library version or a newer Java. |
| `NoClassDefFoundError` for a library class | The library is missing from `dependencies.yml`, or the code ran before async initialization finished. |
| `NoSuchMethodError` in a library | Another plugin's copy of the same library was loaded first. Relocate (see [Relocation](#relocation)). |
| `InaccessibleObjectException` mentioning `java.net` | Class loader injection was blocked. Start the server with `--add-opens java.base/java.net=ALL-UNNAMED`. |
| "Network unreachable and no cached artifact available" | No internet access. Copy the cache from a machine that has it, or point `jedependency.cache.dir` to a filled cache. |
| Two plugins load different JExDependency versions | At least one plugin does not relocate JExDependency. See the [shading guide](#shading-jexdependency-into-your-plugin). |

Turn on `FINE` logging for the `JExDependency` logger to see every download, checksum check and relocation.

## Alternatives

| | Spigot `libraries:` | Paper `PluginLoader` | Shading | JExDependency |
|---|:---:|:---:|:---:|:---:|
| Works on Spigot | 1.16.5+ | no | yes | yes |
| Works on Paper | yes | yes | yes | yes |
| Repositories besides Maven Central | no | yes | n/a | built-in list only (custom: planned) |
| Package relocation | no | no | yes | yes |
| Small plugin jar | yes | yes | no | yes |

[libby](https://github.com/AlessioDP/libby) solves a similar problem with a code-first API across more platforms;
compare both if you also target proxies.

## Roadmap and contributing

Planned work, from opt-in community repositories to build-time lockfiles and parallel resolution, is in
[ROADMAP.md](ROADMAP.md). Items marked **help wanted** are good first contributions; read
[CONTRIBUTING.md](CONTRIBUTING.md) first.

```bash
./gradlew build   # JDK 21+; compiles, runs the tests and builds the Javadoc
```

Questions and ideas: [GitHub Issues](https://github.com/JExcellence/JExDependency/issues), email
[justin.eiletz@jexcellence.de](mailto:justin.eiletz@jexcellence.de) or Discord
[`jexcellence`](https://discord.com/users/jexcellence).

## License

[MIT](LICENSE)
