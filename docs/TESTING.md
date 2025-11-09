# Testing Documentation

## Current Status

⚠️ **Tests are currently disabled** due to compatibility issues that need to be addressed.

## Known Issues

### 1. MockBukkit Import Issues (FIXED ✅)
- **Problem**: Test files were using incorrect package names for MockBukkit v3.x
- **Solution**: Created `.build/fix-mockbukkit-imports.ps1` script to automatically fix imports
- **Status**: Fixed - all imports now use `be.seeseemelk.mockbukkit`

### 2. Missing Test Dependencies
Several test compilation errors remain (100+ errors across all modules):

#### Plugin Annotation Missing
```
Package org.bukkit.plugin.java.annotation.plugin ist nicht vorhanden
```
- The `@Plugin` annotation is not available in the test classpath
- Tests trying to extend `final` classes (RDQFree, RDQPremium)

#### Missing Dependencies
```
Package jakarta.persistence ist nicht vorhanden
Package org.assertj.core.api ist nicht vorhanden
Package net.milkbowl.vault.economy ist nicht vorhanden
```
- Jakarta Persistence API not in test classpath
- AssertJ library missing
- Vault API missing for economy tests

#### Missing Methods
```
Symbol nicht gefunden: getRankManager()
Symbol nicht gefunden: findByAttributes()
Symbol nicht gefunden: findAllAsync()
```
- Some test files reference methods that don't exist in the current codebase
- Tests may be outdated or referencing removed/refactored functionality

#### Type Compatibility Issues
```
Inkompatible Typen: List<CAP#1> kann nicht in List<Object> konvertiert werden
Referenz zu onClick ist mehrdeutig
```
- Mockito type inference issues with generic types
- Method overloading ambiguity in inventory framework

## Building Without Tests

The production build works perfectly without tests:

```bash
# PowerShell
./build.ps1

# Or with Gradle directly
./gradlew build -x test
```

## Fixing Tests (TODO)

To enable tests, the following needs to be addressed:

### High Priority
1. **Remove or fix `@Plugin` annotations** in test classes
2. **Update test classes** to not extend `final` plugin classes
3. **Review and update test methods** that reference non-existent methods like `getRankManager()`

### Medium Priority
4. **Fix Mockito generic type issues** in manager tests
5. **Add missing test dependencies** if required
6. **Update test assertions** to match current API

### Low Priority
7. **Add integration tests** for critical functionality
8. **Set up CI/CD** to run tests automatically
9. **Improve test coverage** for new features

## Test Structure

```
[Module]/src/test/java/
├── [package]/
│   ├── *Test.java          # Unit tests
│   ├── *ImplTest.java      # Implementation tests
│   └── manager/            # Manager tests
```

## Running Tests (When Fixed)

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :RCore:test
./gradlew :RDQ:rdq-free:test
./gradlew :JExEconomy:test

# Run with detailed output
./gradlew test --info

# Run specific test class
./gradlew :JExEconomy:test --tests "de.jexcellence.economy.JExEconomyTest"
```

## Test Dependencies

Current test dependencies (from `libs.versions.toml`):
- **JUnit Jupiter**: 5.10.2
- **Mockito Core**: 5.14.2
- **Mockito JUnit Jupiter**: 5.14.2
- **MockBukkit**: 3.133.2 (for Minecraft 1.21)

## Contributing

When adding new tests:
1. Use `be.seeseemelk.mockbukkit` for MockBukkit imports
2. Follow existing test patterns
3. Ensure tests are independent and can run in any order
4. Mock external dependencies properly
5. Add meaningful assertions with descriptive messages

## Resources

- [MockBukkit Documentation](https://github.com/MockBukkit/MockBukkit)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
