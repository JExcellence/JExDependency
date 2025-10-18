# Agent Guidelines

## Repository Overview
- Root contains top-level documentation and configuration files such as `README.md`, `LICENSE`, and `build-all.bat`.
- Projects utilize multiple implemented utilities including [JEConfig](https://github.com/Antimatter-Zone/JEConfig), [JEHibernate](https://github.com/Antimatter-Zone/JEHibernate), and embedded dependencies shipped within this repository's modules.

## System Architecture
Two main plugins exist in this framework with potential for expansion: `RCore` and `RDQ`. Three jars are created during the build process—`RCore.jar`, `RDQ-Free.jar`, and `RDQ-Premium.jar`. The modules are broken down as follows:

- **JExCommand** – Command framework that auto-registers and implements Minecraft commands for Bukkit, Spigot, and Paper servers. Config options are available for each command.
- **JExDependency** – Runtime dependency management layer for Paper & Spigot plugins that handles downloading, relocating, and shading third-party libraries at runtime.
- **JExEconomy** – Multi-currency economy plugin for Paper servers with internationalization, logging, console administration utilities, and a developer API.
- **JExTranslate** – Internationalization (i18n) library for Minecraft servers built around MiniMessage formatting and compatibility across supported Minecraft versions.
- **RCore** – Core functions of the RDC ecosystem, including database management, encryption/decryption, statistics APIs, and listeners shared across modules.
- **RDQ** – Main gameplay plugin with database entities, permissions, requirements, rewards, GUIs, and commands covering bounties, ranks, perks, quests, and more.
- **RPlatform** – Shared framework implemented across all jar files providing APIs, metrics, placeholders, and reusable views.

## Navigating the Project
1. Start at the repository root (`/workspace/raindropcentral`).
2. Use `ls` to explore major directories:
    - `RDQ/src/main/java/com/raindropcentral/rdq/` – Main feature rich plugin folder.
    - `RCore/src/main/java/com/raindropcentral/rcore/` – Folder for core functions of all plugins.
3. Within each directory, prefer `ls` for listing, and `rg <pattern>` for searching specific symbols or text.
4. Always look for nested `AGENTS.md` files when entering a subdirectory; they may contain additional, more specific conventions.
5. Always review Javadocs and `package-info.java` files for lifecycle notes and invariants before modifying existing classes.

## Module-Specific Guides
- **JExCommand** (`JExCommand/`) – Follows the command framework patterns defined under `com.jex.command`. Keep command registration metadata in sync with paper-plugin descriptors and maintain consistent permission key naming.
- **JExDependency** (`JExDependency/`) – Centralizes runtime dependency declarations. Verify shaded dependencies and relocation rules in Gradle scripts when updating libraries.
- **JExEconomy** (`JExEconomy/`) – Houses economy services, transaction logging, and UI code. Coordinate balance and currency calculations with integration tests located under the module's `src/test/java` directory.
- **JExTranslate** (`JExTranslate/`) – Provides translation bundles and formatting utilities. Keep message keys stable and update language resource files together with code changes.
- **RCore** (`RCore/`) – Contains shared services, configuration bootstrapping, and data access layers. Review service lifecycle hooks and ensure cross-module APIs remain backward compatible.
- **RDQ** (`RDQ/`) – Implements gameplay logic, quests, perks, and GUIs. Maintain alignment between domain models, database schema migrations, and command handlers.
- **RPlatform** (`RPlatform/`) – Offers shared platform abstractions, placeholder APIs, and metrics utilities. Changes here often cascade across other modules; validate integrations before merging.

## Code Style
- Java sources follow an 4-space indentation style rather than tabs. Keep class members, control blocks, and inner scopes aligned using exact four-space offsets as demonstrated in `RCore/src/main/java/com/raindropcentral/rcore/RCore.java`.
- Place opening braces on the same line as the class or method declaration (K&R style), and close braces on their own line aligned with the corresponding declaration.
- Annotate overrides directly above the method signature with no blank line in between, and preserve blank lines between lifecycle methods to mirror the spacing pattern present in the reference file.
- Keep `try`/`catch` blocks vertically aligned: indent the `catch` keyword one level deeper than the `try`, and wrap long parameter lists on new lines while preserving the 8-space indentation for continuation lines.
- Javadocs added to class files should include an author tag. Unless otherwise specified use JExcellence as the author.
- Javadocs added to class files should include a `@since` tag should be 1.0.0 and the `@version` tag should be 1.0.1.
- Javadocs added to methods include any necessary `return` and `param` tags

## Development Flow
1. Identify scope and read any relevant documentation within the class files.
2. Locate the target module inside `RDQ/src/main/java/com/raindropcentral/rdq/` or `RCore/src/main/java/com/raindropcentral/rcore/`.
3. Implement changes following the coding standards described in the closest `AGENTS.md`.
4. Update or add unit tests that cover new or modified functionality.
5. From the repository root run `./gradlew clean build` to ensure all modules compile, lint, and execute their unit tests consistently.
6. Document changes succinctly in commits and PR descriptions.

## Testing Expectations
- Write unit tests alongside feature work or bug fixes to maintain coverage.
- If adding new functionality, include tests that validate both nominal and edge cases.
- Use [MockBukkit]("https://docs.mockbukkit.org/") for unit testing any bukkit, spigot, or paper related items
- Run `./gradlew clean build` for a full validation cycle and `./gradlew test` when iterating quickly on unit coverage.
- Target module builds with commands like `./gradlew :JExCommand:build`, `./gradlew :JExEconomy:build`, or `./gradlew :RDQ:build` depending on where changes occurred, mirroring the compilation order described in the README.
- When verifying packaging or shading steps, rerun `./gradlew :RCore:shadowFree` and, if on Windows, execute `build-all.bat` to confirm artifacts assemble without errors.

## Commit and PR Workflow
1. Review `git status` to confirm the intended files are staged, then use `git add` to stage your changes.
2. Commit with a clear, descriptive message summarizing the work performed.
3. Ensure all relevant Gradle commands listed above have completed successfully before sharing the changes.
4. After committing on the current branch, invoke the `make_pr` tool to generate the pull request summary for reviewers.

## General Tips
- Keep commits focused and descriptive.
- Follow existing patterns and respect linting or formatting rules enforced by the repository.
- Seek additional guidance from maintainers if project-specific testing instructions exist within service directories.
