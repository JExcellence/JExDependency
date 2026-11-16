# Contributing

Bug reports, fixes and focused features are welcome.

## Before you start

- For anything larger than a bug fix, open an issue first so the approach can be agreed before you write code.
- Security problems go through [SECURITY.md](SECURITY.md), not public issues.

## Build and test

```bash
./gradlew build
```

Requires JDK 21 or newer. `build` compiles, runs the tests and builds the Javadoc; all three must pass.

If your change affects loading on a server, say in the pull request which server software, Minecraft version and
Java version you tested on.

## Code style

- Java 21 language level; the published jar must keep running on Java 21.
- No wildcard imports. Nullability annotations from `org.jetbrains.annotations` on public API.
- Javadoc on every public type and method.
- Keep public API backwards compatible within a major version. Deprecate before removing.
- New behaviour comes with tests under `src/test/java`.

## Pull requests

1. Create a branch from `main`.
2. Keep one topic per pull request.
3. Describe what changed, why, and how you verified it.
4. Make sure the CI build is green.

By contributing you agree that your contribution is licensed under the [MIT License](LICENSE).
