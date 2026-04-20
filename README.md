<div align="center">

# JExDependency

**Ship lightweight Paper & Spigot plugins. Let your players download the libraries.**

Runtime dependency resolution, downloading, relocation, and classpath injection — purpose-built for modern Minecraft servers.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Paper](https://img.shields.io/badge/Paper-1.20%2B-blue.svg)](https://papermc.io/)
[![Spigot](https://img.shields.io/badge/Spigot-supported-brightgreen.svg)](https://www.spigotmc.org/)
[![Version](https://img.shields.io/badge/version-2.0.0-informational.svg)](https://github.com/JExcellence/JExDependency/releases)
[![GitHub stars](https://img.shields.io/github/stars/JExcellence/JExDependency?style=social)](https://github.com/JExcellence/JExDependency/stargazers)

[Quick Start](#-quick-start) • [Why JExDependency](#-why-jexdependency) • [Features](#-features) • [Configuration](#%EF%B8%8F-configuration) • [FAQ](#-faq)

</div>

---

## 💡 Why JExDependency

Minecraft plugin jars keep getting larger. Hibernate, Jackson, Caffeine, JDBC drivers — shading them all bloats your jar to 20 MB+ and guarantees classpath collisions the moment another plugin ships a different version.

**JExDependency flips that model.** Declare dependencies in a tiny YAML file, ship a 50 KB plugin, and let the library download, verify, (optionally) relocate, and inject the JARs at runtime — on every flavour of Paper and Spigot, transparently.

> Think of it as **Paper's `libraries` block, but portable, relocation-aware, and working on Spigot too.**

---

## ✨ Features

- 🧩 **Universal loader** — Paper plugin-loader handshake on 1.20+, legacy bootstrap on Spigot/older Paper. Auto-detected.
- 📦 **YAML-first descriptors** — merge generic / Paper-only / Spigot-only files, deduplicate versions, normalise coordinates.
- 🔀 **Optional ASM relocation** — isolate conflicting packages without paying the CPU cost when you don't need it.
- ⚡ **Sync or async bootstrap** — block `onLoad()` for simplicity, or return a `CompletableFuture` for non-blocking startup.
- 🔐 **Checksum-verified downloads** — corrupted artifacts are retried; temp dirs are wiped on every exit path.
- ☕ **Java 21 ready** — module de-encapsulation and `--add-opens` semantics handled for reflective libraries.
- 🧹 **Deterministic cache** — artefacts live under `plugins/<Plugin>/libraries/` and `.../libraries/remapped/`; safe to prune.
- 🪵 **First-class logging** — every bootstrap cycle reports counts, timings, and redacted paths through the plugin logger.

---

## 🚀 Quick Start

### 1. Add the dependency

Replace `VERSION` with the latest [release tag](https://github.com/JExcellence/JExDependency/releases).

<details open>
<summary><b>Gradle (Kotlin DSL)</b></summary>

```kotlin
repositories {
    maven("https://repo.jexcellence.de/releases")
}

dependencies {
    implementation("de.jexcellence.dependency:jexdependency:VERSION")
}
```

</details>

<details>
<summary><b>Gradle (Groovy)</b></summary>

```groovy
repositories {
    maven { url 'https://repo.jexcellence.de/releases' }
}

dependencies {
    implementation 'de.jexcellence.dependency:jexdependency:VERSION'
}
```

</details>

<details>
<summary><b>Maven</b></summary>

```xml
<repositories>
    <repository>
        <id>jexcellence</id>
        <url>https://repo.jexcellence.de/releases</url>
    </repository>
</repositories>

<dependency>
    <groupId>de.jexcellence.dependency</groupId>
    <artifactId>jexdependency</artifactId>
    <version>VERSION</version>
</dependency>
```

</details>

### 2. Declare runtime libraries

Create `src/main/resources/dependency/dependencies.yml`:

```yaml
dependencies:
  - "com.github.ben-manes.caffeine:caffeine:3.2.2"
  - "com.fasterxml.jackson.core:jackson-databind:2.18.2"
  - "com.mysql:mysql-connector-j:9.2.0"
```

Need platform-specific sets? Add `dependencies-paper.yml` or `dependencies-spigot.yml` — they are merged automatically.

### 3. Bootstrap in your plugin

```java
public final class ExamplePlugin extends JavaPlugin {
    @Override
    public void onLoad() {
        // Synchronous — simplest; blocks onLoad while JARs download.
        JEDependency.initialize(this, ExamplePlugin.class);
    }
}
```

That's it. Libraries land under `plugins/ExamplePlugin/libraries/` and are injected into your plugin's classloader before `onEnable()` fires.

---

## 🎯 Bootstrap modes

| Method | Blocking | Forces relocation | When to use |
| --- | :---: | :---: | --- |
| `JEDependency.initialize(plugin, anchor)` | ✅ | — | Default. Safe inside `onLoad()`. |
| `JEDependency.initializeWithRemapping(plugin, anchor)` | ✅ | ✅ | You need guaranteed isolation from other plugins' libs. |
| `JEDependency.initializeAsync(plugin, anchor)` | ❌ | — | Non-blocking startup; await the returned `CompletableFuture<Void>` before touching injected classes. |

All three methods accept an optional `String[]` of extra Maven coordinates (`group:artifact:version[:classifier]`) appended to the YAML list.

---

## ⚙️ Configuration

JExDependency is driven by JVM system properties so server operators can tweak behaviour without recompiling your plugin.

| Property | Default | Purpose |
| --- | --- | --- |
| `-Djedependency.remap` | `false` | `true` / `1` / `yes` / `on` forces ASM relocation. |
| `-Djedependency.relocations` | — | Comma-separated `pattern=target` overrides, e.g. `com.google.gson=mypkg.libs.gson`. |
| `-Djedependency.relocations.prefix` | — | Global prefix applied to auto-relocated packages. |
| `-Djedependency.relocations.excludes` | — | Packages that must never be relocated. |

---

## 🧠 How it works

```
┌──────────────────────────────────────────────────────────────────┐
│  onLoad()  →  JEDependency.initialize(...)                       │
└──────────────┬───────────────────────────────────────────────────┘
               ▼
   ┌───────────────────────┐     Paper 1.20+?
   │  Server detection     │────────┐
   └───────────────────────┘        ▼
               │            ┌───────────────────────┐
               │            │ Inject pre-downloaded │
               │            │ libs via Paper loader │
               │            └───────────┬───────────┘
               ▼                        │
   ┌───────────────────────┐            │
   │  Merge YAML sources   │◀───────────┘
   │  (generic + platform) │
   └───────────┬───────────┘
               ▼
   ┌───────────────────────┐
   │  Resolve + download   │  → checksum verify → cache under
   │  from Maven repos     │    plugins/<Plugin>/libraries/
   └───────────┬───────────┘
               ▼
   ┌───────────────────────┐  (only when -Djedependency.remap=true
   │  Optional ASM remap   │   or initializeWithRemapping(...) is used)
   └───────────┬───────────┘
               ▼
   ┌───────────────────────┐
   │  URLClassLoader       │  → classes visible to your plugin
   │  injection            │    before onEnable() fires.
   └───────────────────────┘
```

---

## 📁 Project layout

```
src/main/java/de/jexcellence/dependency/
├── JEDependency.java          ← public entrypoints
├── manager/DependencyManager  ← core resolution pipeline
├── remapper/                  ← ASM relocation (opt-in)
├── downloader/                ← Maven artefact retrieval + checksum
├── injector/ClasspathInjector ← runtime classloader injection
├── loader/                    ← Paper / Spigot loader adapters
├── repository/                ← repository registry + mirrors
├── resolver/                  ← coordinate + transitive resolution
└── model/                     ← immutable data types
```

Full Javadoc lives next to the sources. Start from [`JEDependency`](src/main/java/de/jexcellence/dependency/JEDependency.java) and [`DependencyManager`](src/main/java/de/jexcellence/dependency/manager/DependencyManager.java).

---

## 🔍 Observability

- Every stage logs through `plugin.getLogger()` — no custom appenders required.
- FINE level reveals per-artifact download progress, checksum results, and relocation summaries.
- Failure paths sanitise file system roots so logs are safe to share.
- Start / end timestamps and dependency counts are emitted for automation to diff across restarts.

---

## 🔐 Security practices

- Maven checksum validation on every downloaded jar; corrupted files trigger a retry and cache purge.
- Remapping runs inside a sandboxed `URLClassLoader` so a half-relocated class can never leak into your plugin loader on failure.
- Temporary directories are wiped on both success and failure — no stale bytecode survives restarts.

---

## ❓ FAQ

<details>
<summary><b>How is this different from Paper's built-in <code>libraries</code> block?</b></summary>
<br>
Paper's loader only works on Paper 1.20+ and gives you no relocation support. JExDependency runs on Spigot and older Paper too, adds optional ASM relocation, and — when the Paper loader <i>is</i> active — cooperates with it instead of duplicating work.
</details>

<details>
<summary><b>Will it download every startup?</b></summary>
<br>
No. Artefacts are cached under the plugin's data folder and reused across restarts. Only missing or checksum-invalid jars are re-downloaded.
</details>

<details>
<summary><b>Does async mode block <code>onEnable()</code>?</b></summary>
<br>
No. <code>initializeAsync</code> returns a <code>CompletableFuture&lt;Void&gt;</code> immediately. You decide whether to <code>.join()</code> before using the dependencies or to schedule work after completion.
</details>

<details>
<summary><b>What Java versions are supported?</b></summary>
<br>
Java 21 is the primary target. Module de-encapsulation and <code>--add-opens</code> semantics for reflective libraries are handled for you.
</details>

<details>
<summary><b>Does relocation rewrite my plugin's own classes?</b></summary>
<br>
No. Only downloaded dependency jars are visited. Your plugin bytecode is never touched.
</details>

---

## 💬 Support & Contact

Need a hand, found a bug, or want to bounce ideas around?

- 📧 **Email** — [justin.eiletz@jexcellence.de](mailto:justin.eiletz@jexcellence.de)
- 💬 **Discord** — [`jexcellence`](https://discord.com/users/jexcellence)
- 🐛 **Issues** — [GitHub Issues](https://github.com/JExcellence/JExDependency/issues) for bugs and feature requests

---

## 🤝 Contributing

Issues, discussions, and PRs are welcome.

1. Fork the repo and create a feature branch.
2. Run `./gradlew build` to verify the project compiles.
3. Add tests or a reproduction case where applicable.
4. Open a PR describing the change and the motivation.

Please follow the existing code style — no wildcard imports, Javadoc on public API, nullability annotations from `org.jetbrains.annotations`.

---

## 📜 License

Released under the [MIT License](LICENSE). Use it, ship it, star it. ⭐

---

<div align="center">

**Built with care by [JExcellence](https://github.com/JExcellence).**
If JExDependency saves you a shaded jar today, consider giving it a star — it genuinely helps others find the project.

</div>
