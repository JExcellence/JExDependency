# Agent Guidelines

## Repository Overview
- Root contains top-level documentation and configuration files such as `README.md`, `LICENSE`, and `build-all.bat`.
- Projects utilize multiple implemented utilities including [JEConfig](https://github.com/Antimatter-Zone/JEConfig), [JEHibernate](https://github.com/Antimatter-Zone/JEHibernate), and embedded dependencies such as `JECurrency`, `R18n`, `RCommands`, and `RPlatform`.

## System Architecture
Two main plugins exist in this framework with potential for expansion. `RCore` and `RDQ`. Three jars are created during the build process `RCore.jar`, `RDQ-Free.jar`, and `RDQ-Premium.jar`. The modules are broken down as follows:

- **JExCommand** -Command framework which auto registers and implements MC commands for Bukkit, Spigot, and Paper servers. Config options available for each command.
- **JExDependency** - Modern runtime dependency management for Minecraft Paper & Spigot plugins.
- **JExEconomy** - Multi-currency economy plugin for Paper servers with a modern UI, internationalization, detailed logging, console administration utilities, and a developer API.
- **JExTranslate** - Modern internationalization (i18n) library for Minecraft servers. Built with 2025 Java standards, it provides comprehensive multilingual support with advanced MiniMessage formatting, robust validation, and seamless compatibility across all Minecraft versions from 1.8 to 1.21.8.
- **RCore** – Core functions of the RDC ecosystem. Offering database management, encryption and decryption, statistics API, and listeners.
- **RDQ** – Main plugin/module which players interact with. Plugin specific database class entities, permissions, requirements, rewards, GUI classes, and commands. Offering bounties, ranks, perks, quests, and more.
- **RPlatform** – Framework implemented in all jar files, offers the API, metrics, placeholder, and views necessary in all plugins.

## Navigating the Project
1. Start at the repository root (`/workspace/raindropcentral`).
2. Use `ls` to explore major directories:
    - `RDQ/src/main/java/com/raindropcentral/rdq/` – Main feature rich plugin folder.
    - `RCore/src/main/java/com/raindropcentral/rcore/` – Folder for core functions of all plugins.
3. Within each directory, prefer `ls` for listing, and `rg <pattern>` for searching specific symbols or text.
4. Always look for nested `AGENTS.md` files when entering a subdirectory; they may contain additional, more specific conventions.
5. Before modifying the JECurrency module, read `JECurrency/AGENTS.md` for lifecycle, resource, and testing guidance specific to that plugin.
6. Always review javadocs and `package-info.java` files

## Module-Specific Guides
- `RaindropQuests/AGENTS.md` – Minecraft Plugin module package, entity/converter patterns, GUI and command conventions, and resource/Gradle checklists.
- `RCore/AGENTS.md` - module-specific lifecycle notes, descriptor expectations, and formatting reminders before making changes.

## Code Style
- Java sources follow an 4-space indentation style rather than tabs. Keep class members, control blocks, and inner scopes aligned using exact eight-space offsets as demonstrated in `RCore/src/main/java/com/raindropcentral/rcore/RCore.java`.
- Place opening braces on the same line as the class or method declaration (K&R style), and close braces on their own line aligned with the corresponding declaration.
- Annotate overrides directly above the method signature with no blank line in between, and preserve blank lines between lifecycle methods to mirror the spacing pattern present in the reference file.
- Keep `try`/`catch` blocks vertically aligned: indent the `catch` keyword one level deeper than the `try`, and wrap long parameter lists on new lines while preserving the 8-space indentation for continuation lines.

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
- Target module builds with commands like `./gradlew :JECurrency:build`, `./gradlew :R18n:build`, or `./gradlew :RaindropQuests:build` depending on where changes occurred, mirroring the compilation order described in the README.
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
