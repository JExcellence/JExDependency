# Requirements Document

## Introduction

The CurrencyRequirement class in RPlatform is experiencing a NoSuchMethodException when attempting to retrieve player balances from JExEconomy. The current implementation uses reflection unnecessarily, even though RPlatform has JExEconomy as a compile-time dependency. This causes method signature mismatches and makes the code harder to maintain.

## Glossary

- **CurrencyRequirement**: A requirement type in RPlatform that checks if a player has sufficient currency balance
- **CurrencyAdapter**: The JExEconomy service class that provides currency operations
- **ServiceManager**: Bukkit's service registry for plugin-provided services
- **OfflinePlayer**: Bukkit interface representing a player who may or may not be online
- **Player**: Bukkit interface representing an online player (extends OfflinePlayer)
- **compileOnly**: Gradle dependency scope that provides classes at compile-time but not bundled at runtime

## Requirements

### Requirement 1

**User Story:** As a server administrator, I want currency requirements to work correctly with JExEconomy, so that players can see their progress toward currency-based perks and ranks.

#### Acceptance Criteria

1. WHEN THE CurrencyRequirement_System retrieves a player balance, THE CurrencyRequirement_System SHALL invoke the CurrencyAdapter getBalance method directly with OfflinePlayer and Currency parameters
2. WHEN THE CurrencyRequirement_System invokes the withdraw method, THE CurrencyRequirement_System SHALL call CurrencyAdapter withdraw directly with OfflinePlayer, Currency, and double parameters
3. WHEN THE CurrencyRequirement_System invokes the deposit method, THE CurrencyRequirement_System SHALL call CurrencyAdapter deposit directly with OfflinePlayer, Currency, and double parameters
4. WHEN THE CurrencyRequirement_System encounters a ClassNotFoundException for JExEconomy classes, THE CurrencyRequirement_System SHALL fall back to Vault gracefully
5. WHEN THE CurrencyRequirement_System successfully retrieves a balance, THE CurrencyRequirement_System SHALL return the balance value without errors

### Requirement 2

**User Story:** As a developer, I want to use direct API calls instead of reflection, so that the code is type-safe, maintainable, and performs better.

#### Acceptance Criteria

1. THE CurrencyRequirement_System SHALL use Bukkit ServiceManager to retrieve the CurrencyAdapter service
2. THE CurrencyRequirement_System SHALL call CurrencyAdapter methods directly without reflection
3. THE CurrencyRequirement_System SHALL handle ClassNotFoundException when JExEconomy is not present at runtime
4. THE CurrencyRequirement_System SHALL fall back to Vault when JExEconomy classes are not available
5. THE CurrencyRequirement_System SHALL avoid unnecessary try-catch blocks for method invocation

### Requirement 3

**User Story:** As a server administrator, I want clear error messages when currency operations fail, so that I can diagnose and fix configuration issues.

#### Acceptance Criteria

1. WHEN THE CurrencyRequirement_System fails to find a currency, THE CurrencyRequirement_System SHALL log the currency identifier and list available currencies
2. WHEN THE CurrencyRequirement_System encounters a ClassNotFoundException, THE CurrencyRequirement_System SHALL log that JExEconomy is not available
3. WHEN THE CurrencyRequirement_System falls back to Vault, THE CurrencyRequirement_System SHALL log the reason for fallback at FINE level
4. WHEN THE CurrencyRequirement_System completes an operation successfully, THE CurrencyRequirement_System SHALL log at FINE level to avoid log spam
5. THE CurrencyRequirement_System SHALL use the centralized logging system for all log messages
