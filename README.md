<div align="center">

# JExDependency

**Ship small Spigot, Paper and Folia plugins. Download the libraries at startup instead of shading them.**

[![Build](https://github.com/JExcellence/JExDependency/actions/workflows/ci.yml/badge.svg)](https://github.com/JExcellence/JExDependency/actions/workflows/ci.yml)
[![JitPack](https://jitpack.io/v/JExcellence/JExDependency.svg)](https://jitpack.io/#JExcellence/JExDependency)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 21+](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://adoptium.net/)

[Quick start](#quick-start) · [Paper plugins](#paper-plugins-paper-pluginyml) · [Shading](#shading-jexdependency-into-your-plugin) · [Configuration](#configuration) · [Cache](#where-files-go) · [Security](#security-model) · [Troubleshooting](#troubleshooting)

</div>

---

## What it does

Hibernate, Jackson, Caffeine and JDBC drivers turn a 200 KB plugin into a 20 MB jar, and two plugins that shade
different versions of the same library break each other. JExDependency reads a list of Maven coordinates from your
plugin jar, downloads the jars once per server, checks them, optionally relocates their packages, and puts them on
your plugin's classpath before `onEnable()`.

- **Spigot and Paper.** On Paper it hooks into the plugin loader (`paper-plugin.yml`); on Spigot and `plugin.yml`
  plugins it injects into the plugin class loader.
- **Shared download cache.** Every plugin on the server that uses JExDependency shares one cache, so a library is
  downloaded once. Downloads are locked per artifact, so plugins starting in parallel never download the same file twice.
- **Transitive resolution.** Dependencies declared in each artifact's POM are resolved too.
- **Optional relocation.** ASM-based package relocation at runtime, the same result as Shadow's `relocate`, without
  shading anything into your jar.
- **Retries and checksums.** Transient HTTP errors are retried with backoff; a confirmed SHA-1 mismatch rejects the jar.
- **Failure policy.** Choose whether a missing library stops the plugin, is skipped, or is handed to your own callback.

## Compatibility

| | Status |
|---|---|
| Java | 21 or newer (the jar is compiled for Java 21) |
| Paper | Used in production on Paper 26.x |
| Spigot, Folia | Supported by design; automated real-server tests are planned and not in place yet |
| Libraries you load | Must run on the server's Java version. A Java 25 library will not load on a Java 21 server. |

## Quick start

### 1. Add the dependency

Releases are built by [JitPack](https://jitpack.io/#JExcellence/JExDependency). Replace `TAG` with a
[release tag](https://github.com/JExcellence/JExDependency/releases).

<details open>
<summary><b>Gradle (Kotlin DSL)</b></summary>

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.JExcellence:JExDependency:TAG")
}
```

</details>

<details>
<summary><b>Maven</b></summary>

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
    <version>TAG</version>
</dependency>
```

</details>

JExDependency itself (plus ASM) must be inside your plugin jar, so shade it. See
[Shading JExDependency into your plugin](#shading-jexdependency-into-your-plugin).

### 2. Declare runtime libraries

Create `src/main/resources/dependency/dependencies.yml`:

```yaml
dependencies:
  - "com.github.ben-manes.caffeine:caffeine:3.2.2"
  - "com.fasterxml.jackson.core:jackson-databind:2.18.2"
  - "com.mysql:mysql-connector-j:9.2.0"
```

Libraries that only one platform needs go into `dependency/paper/dependencies.yml` or
`dependency/spigot/dependencies.yml`; they are merged with the main list. Keep these libraries as `compileOnly`
in your build so they are not shaded.

### 3. Start it in `onLoad()`

```java
public final class ExamplePlugin extends JavaPlugin {
    @Override
    public void onLoad() {
        JEDependency.initialize(this, ExamplePlugin.class);
    }
}
```

The libraries are on your plugin's classpath when `onEnable()` runs.

| Method | Blocks | Relocates | Use it when |
|---|:---:|:---:|---|
| `JEDependency.initialize(plugin, anchor)` | yes | no | Default. |
| `JEDependency.initializeWithRemapping(plugin, anchor)` | yes | yes | Your libraries may clash with other plugins' copies. |
| `JEDependency.initializeAsync(plugin, anchor)` | no | no | You want startup to continue; wait for the returned `CompletableFuture<Void>` before using the libraries. |

Each method also accepts a `String[]` of extra coordinates (`group:artifact:version[:classifier]`).

## Paper plugins (`paper-plugin.yml`)

Paper plugins can resolve libraries before the plugin class is even loaded. Register the loader:

```yaml
name: ExamplePlugin
version: 1.0.0
main: com.example.ExamplePlugin
api-version: "1.21"
loader: de.jexcellence.dependency.loader.PaperPluginLoader
```

If you relocate JExDependency (recommended, see below), use the relocated class name, for example
`com.example.libs.jexdependency.loader.PaperPluginLoader`. Still call `JEDependency.initialize(...)` in `onLoad()`;
it detects the work the loader already did and only fills in what is missing.

## Shading JExDependency into your plugin

Relocate JExDependency and ASM into your own package. On Spigot, plugin class loaders can see each other's classes,
so two plugins that ship different JExDependency versions under the same package can load the wrong one.

```kotlin
plugins {
    id("com.gradleup.shadow") version "9.4.0"
}

dependencies {
    implementation("com.github.JExcellence:JExDependency:TAG")
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.2.2") // loaded at runtime, not shaded
}

tasks.shadowJar {
    relocate("de.jexcellence.dependency", "com.example.libs.jexdependency")
    relocate("org.objectweb.asm", "com.example.libs.asm")
}
```

### Replacing your existing relocate block

If your build shades libraries and relocates them:

```kotlin
tasks.shadowJar {
    relocate("com.github.benmanes", "com.example.libs.caffeine")
    relocate("com.cryptomorin.xseries", "com.example.libs.xseries")
}
```

move those libraries to `compileOnly`, list them in `dependency/dependencies.yml`, and start with
`JEDependency.initializeWithRemapping(this, ExamplePlugin.class)`. The relocation then happens on the server and
your jar stays small. The target package is controlled by the properties below.

## Configuration

Server owners can change behaviour with JVM system properties, without rebuilding the plugin.

| Property | Default | Effect |
|---|---|---|
| `jedependency.remap` | `false` | `true`, `1`, `yes` or `on` turns on relocation for every plugin using JExDependency. |
| `jedependency.relocations` | none | Explicit relocations, comma separated: `com.google.gson=>my.libs.gson,org.foo=>my.libs.foo`. |
| `jedependency.relocations.prefix` | none | Package prefix for automatic relocation. |
| `jedependency.relocations.excludes` | none | Comma-separated packages that are never relocated. |
| `jedependency.cache.dir` | `<server>/.jexdependency/cache` | Location of the shared download cache (useful for Docker volumes). |
| `jedependency.path.default` | `/dependency/dependencies.yml` | Resource path of the main dependency list. |
| `jedependency.path.paper` | `/dependency/paper/dependencies.yml` | Resource path of the Paper-only list. |
| `jedependency.path.spigot` | `/dependency/spigot/dependencies.yml` | Resource path of the Spigot-only list. |

Example: `java -Djedependency.remap=true -jar paper.jar`

### Failure policy

```java
new DependencyManager(this, ExamplePlugin.class)
        .setFailurePolicy(FailurePolicy.DEGRADED)
        .initialize(null);
```

| Policy | Behaviour when a library cannot be loaded |
|---|---|
| `STRICT` | Startup fails with an exception. |
| `DEGRADED` | The library is skipped and logged; the plugin continues. |
| `CALLBACK` | Your `FailureCallback` decides. |

## Where files go

```
<server>/.jexdependency/cache/          shared by all plugins
  cache-format.properties               cache layout version
  jars/<group path>/<artifact>/<version>/<artifact>-<version>.jar
  poms/...                              POMs used for transitive resolution
  locks/                                one empty lock file per artifact
plugins/<YourPlugin>/libraries/remapped/<set>/  relocated copies, one folder per relocation set
plugins/<YourPlugin>/libraries/paper-loader-classpath.txt  jars the Paper loader added (Paper only)
```

Deleting the cache is safe while the server is stopped; everything is downloaded again on the next start. The same
goes for `libraries/remapped/`: folders of relocation sets that are no longer used, and jars left directly in it by
versions before 2.2.0, are never loaded and can be removed. Jars
found in the old per-plugin `plugins/<YourPlugin>/libraries/` folder are copied into the shared cache
automatically; the old folder can be deleted afterwards.

If a newer JExDependency version ever changes the cache layout, older versions notice it through
`cache-format.properties` and keep their files in a separate `format-<n>` folder instead of mixing layouts.

## Security model

JExDependency downloads code and runs it inside your server with the same rights as your plugin. Know what that means:

- Jars come from repositories you add at runtime (tried first) and then, in this order, from Maven Central,
  NeetGames Nexus, JitPack, Auxilor, Tcoded, PaperMC, the SpigotMC snapshot repository and the Gradle plugin
  portal. The first repository that has the artifact wins, so an artifact that is not on Maven Central can be served
  by any of the community repositories. All of them are fetched over HTTPS.
- After a download, the published `.sha1` file is compared with the jar. A mismatch rejects the jar. If a repository
  publishes no checksum, the jar is accepted, so only use repositories you trust.
- Pin exact versions in `dependencies.yml`. Version ranges are not supported, which keeps what you tested and what
  servers download identical.
- Network connections: only the repository hosts above, only at startup, only for jars and POMs missing from the
  cache.

Report vulnerabilities as described in [SECURITY.md](SECURITY.md).

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `UnsupportedClassVersionError` | A library needs a newer Java than the server runs. Use an older library version or update Java. |
| `NoClassDefFoundError` for a library class | The library is missing from `dependencies.yml`, or you call it before initialization finished (async mode). |
| `InaccessibleObjectException` mentioning `java.net` | Class loader injection was blocked. Start the server with `--add-opens java.base/java.net=ALL-UNNAMED`. |
| "Network unreachable and no cached artifact available" | The server has no internet access. Copy the cache folder from a machine that has, or set `jedependency.cache.dir` to a pre-filled folder. |
| Two plugins load different versions of JExDependency | At least one of them does not relocate it. See [Shading](#shading-jexdependency-into-your-plugin). |

Turn on `FINE` logging for `JExDependency` to see each download, checksum result and relocation.

## How it compares

| | Spigot `libraries:` | Paper `PluginLoader` | Shading | JExDependency |
|---|:---:|:---:|:---:|:---:|
| Works on Spigot | yes (1.16.5+) | no | yes | yes |
| Works on Paper | yes | yes | yes | yes |
| Repositories other than Maven Central | no | yes | n/a | yes |
| Package relocation | no | no | yes | yes |
| Small plugin jar | yes | yes | no | yes |
| Shared cache across plugins | yes | yes | n/a | yes |

## Building from source

```bash
./gradlew build
```

Requires JDK 21 or newer. Javadoc for each release is published at
<https://jexcellence.github.io/JExDependency/>.

## Contributing and support

Read [CONTRIBUTING.md](CONTRIBUTING.md). Bugs and feature requests go to
[GitHub Issues](https://github.com/JExcellence/JExDependency/issues). Questions: email
[justin.eiletz@jexcellence.de](mailto:justin.eiletz@jexcellence.de) or Discord [`jexcellence`](https://discord.com/users/jexcellence).

## License

[MIT](LICENSE)
