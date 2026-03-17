## Tooling & Shell Usage
- Prefer the bundled bash helpers (`bash -lc`) when invoking shell commands; always set the `workdir` parameter.
- Use `rg`/`rg --files` for searches; fall back only if unavailable.
- Avoid PowerShell-specific commands.

## Language Support
- Always add I18n keys and values to the modules en_US.yml in the resources/translations folder
- Include colors, gradients, or symbols to language values
- Placeholders can be surrounded by %%, and fallback use {}

# Translations
- When translating Strings from the en_US.yml:
  - Preserve placeholders surrounded by %% or {}
  - Maintain color codes surrounded by <>
  - Do not change any keys

# Java Syntax & Documentation Conventions

This repository follows strict Java coding and documentation standards. Any AI agent contributing code MUST comply with the rules below.

## 1) Language + Tooling
- Java version: **Java 24**
- Build tool: **Gradle (Kotlin DSL)** unless otherwise stated by the repo.
- Prefer modern Java features where appropriate (records, switch expressions, `var` where readable, sealed types when useful).

## 2) Hard Requirements (Non-Negotiable)
### 2.1 Javadoc is mandatory
- Follow Google Java Style and Google Javadoc guidance for all public API documentation.
- **Every public class, record, interface, enum, annotation, and public method MUST have meaningful Javadoc.**
- Package-level Javadoc MUST exist via `package-info.java` for each package that exposes public top-level API types.
- Start each Javadoc block with a clear summary sentence. For additional paragraphs, use `<p>`.
- Use `@param`, `@return`, and `@throws` when applicable. Do not add tags that do not apply.
- Use inline tags correctly, especially `{@code}` for literals and `{@link}` for references.
- Favor meaningful documentation over boilerplate text.
- Unchanged `@Override` methods may rely on inherited documentation. Use `/** {@inheritDoc} */` when local clarification is needed.

### 2.2 License header is mandatory for new class files
- Every newly created Java class-source file (`.java`) MUST start with this exact header block before the `package` declaration:
```java
/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */
```

### 2.3 Zero warnings policy
All code changes MUST build and generate Javadocs with **no warnings**.
- No compiler warnings.
- No Javadoc warnings.
- No unchecked/rawtypes warnings.
- No deprecated API warnings unless explicitly approved and documented.

If any warning is unavoidable:
- Do NOT ignore it.
- Fix it or refactor to remove it.
- If truly impossible, document why in code comments + in the PR/commit message, and keep the warning scope minimal.

## 3) Style Conventions
### 3.1 Formatting
- 4 spaces indentation, no tabs.
- One top-level public type per file.
- Braces on the same line (K&R style).
- `final` for constants and where it improves clarity.
- Prefer early returns to deep nesting.

### 3.2 Naming
- Classes/Records/Interfaces/Enums: `PascalCase`
- Methods/fields/locals: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Packages: `lowercase.with.dots`
- Avoid abbreviations unless industry-standard.

### 3.3 Imports
- No wildcard imports.
- Remove unused imports.
- Order: `java.*`, `javax.*`, then third-party, then project packages.

### 3.4 Nullability
- Avoid returning `null`. Prefer `Optional` when appropriate.
- Validate public method arguments and document expectations in JavaDoc.

### 3.5 Exceptions + Logging
- Throw specific exceptions; never swallow exceptions silently.
- If logging exists in the codebase, log with actionable context (but do not leak secrets).

## 4) API Design Rules
- Keep public API minimal and stable.
- Favor immutability for DTOs and configuration objects.
- Prefer interfaces for extensibility when there are multiple implementations.
- Document thread-safety expectations in JavaDoc when relevant.

## 5) Testing Expectations
- Add/update tests for behavior changes.
- Tests must be deterministic and not depend on network/time unless explicitly designed for it.

## 6) Required Verification Commands
After making changes, the agent MUST attempt to run the following commands locally and ensure **zero warnings**:

### Gradle (preferred)
- Run style verification: `./gradlew clean verifyGoogleJavaStyle`
- Use project specific building such as `./gradlew clean RDR:build`
- Use project specific Javadocs such as `./gradlew RDR:javadoc` (or the repo’s Javadoc task if named differently)

If the repo uses multi-module builds, run at root and ensure all modules succeed.

## 7) What to do if commands fail
- Fix the underlying issue; do not “work around” by disabling checks.
- If a task name differs, discover the correct task (`./gradlew tasks`) and run the closest equivalents.
- If environment constraints prevent execution, state exactly what you could not run and why, and still ensure changes are consistent with the zero-warnings policy.

## 8) Output Expectations for AI Agents
When submitting changes, include:
- A short summary of changes
- The commands run (and their results)
- Confirmation that compiler + Javadoc warnings are **zero**
