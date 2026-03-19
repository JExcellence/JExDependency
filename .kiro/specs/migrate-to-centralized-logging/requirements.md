# Requirements Document

## Introduction

This specification defines the requirements for migrating all existing logging in RPlatform and RDQ projects to use the new centralized logging system (CentralLogger and PluginLogger). The migration ensures consistent logging behavior, proper file rotation, UTF-8 encoding, and recursion protection across all modules.

## Glossary

- **CentralLogger**: The main facade class for the centralized logging system that manages plugin-specific loggers
- **PluginLogger**: A plugin-specific logger instance with file rotation and console filtering capabilities
- **JavaPlugin**: The Bukkit plugin instance required by CentralLogger.getLogger()
- **Logger**: The java.util.logging.Logger class used in legacy code
- **Migration**: The process of replacing old logging patterns with the new centralized logging system

## Requirements

### Requirement 1: Identify All Logging Usage

**User Story:** As a developer, I want to identify all existing logging usage in RPlatform and RDQ, so that I can ensure complete migration coverage

#### Acceptance Criteria

1. THE Migration_System SHALL scan all Java files in RPlatform module for logging usage
2. THE Migration_System SHALL scan all Java files in RDQ modules (rdq-common, rdq-premium) for logging usage
3. THE Migration_System SHALL identify all Logger field declarations
4. THE Migration_System SHALL identify all getLogger() method calls
5. THE Migration_System SHALL identify all direct System.out and System.err usage

### Requirement 2: Replace Logger Declarations

**User Story:** As a developer, I want all Logger field declarations replaced with PluginLogger, so that all classes use the centralized logging system

#### Acceptance Criteria

1. THE Migration_System SHALL replace `private static final Logger LOGGER` declarations with `private final PluginLogger logger`
2. THE Migration_System SHALL remove static modifier from logger fields
3. THE Migration_System SHALL update import statements to use PluginLogger instead of java.util.logging.Logger
4. THE Migration_System SHALL ensure logger fields are initialized via constructor injection or CentralLogger.getLogger()

### Requirement 3: Fix CentralLogger.getLogger() Calls

**User Story:** As a developer, I want all CentralLogger.getLogger() calls to use JavaPlugin instances, so that the code compiles without errors

#### Acceptance Criteria

1. WHEN CentralLogger.getLogger() is called with a String parameter, THE Migration_System SHALL replace it with a JavaPlugin instance
2. WHEN CentralLogger.getLogger() is called with a Class parameter, THE Migration_System SHALL replace it with a JavaPlugin instance
3. THE Migration_System SHALL ensure each class has access to a JavaPlugin instance through constructor injection or field access
4. THE Migration_System SHALL update method signatures to accept PluginLogger or JavaPlugin parameters where needed

### Requirement 4: Update Logging Method Calls

**User Story:** As a developer, I want all logging method calls updated to use PluginLogger API, so that logging behavior is consistent

#### Acceptance Criteria

1. THE Migration_System SHALL replace `logger.log(Level.INFO, message)` with `logger.info(message)`
2. THE Migration_System SHALL replace `logger.log(Level.WARNING, message)` with `logger.warning(message)`
3. THE Migration_System SHALL replace `logger.log(Level.SEVERE, message)` with `logger.severe(message)`
4. THE Migration_System SHALL replace `logger.log(Level.FINE, message)` with `logger.debug(message)`
5. THE Migration_System SHALL replace `logger.log(level, message, throwable)` with appropriate PluginLogger methods

### Requirement 5: Handle Classes Without Plugin Access

**User Story:** As a developer, I want classes without direct plugin access to receive logger instances, so that all classes can log properly

#### Acceptance Criteria

1. WHEN a class does not have JavaPlugin access, THE Migration_System SHALL add a PluginLogger constructor parameter
2. THE Migration_System SHALL update all instantiation sites to pass the logger instance
3. THE Migration_System SHALL document the logger parameter in constructor JavaDoc
4. THE Migration_System SHALL ensure logger instances are properly propagated through the object graph

### Requirement 6: Remove PlatformLogger References

**User Story:** As a developer, I want all PlatformLogger references removed, so that only the standard CentralLogger/PluginLogger API is used

#### Acceptance Criteria

1. THE Migration_System SHALL replace all PlatformLogger references with PluginLogger
2. THE Migration_System SHALL remove PlatformLogger class file if it exists
3. THE Migration_System SHALL update all imports from PlatformLogger to PluginLogger
4. THE Migration_System SHALL ensure no compilation errors remain after removal

### Requirement 7: Verify Build Success

**User Story:** As a developer, I want the build to succeed after migration, so that I can confirm all logging is properly migrated

#### Acceptance Criteria

1. THE Migration_System SHALL compile RPlatform module without errors
2. THE Migration_System SHALL compile RDQ modules without errors
3. THE Migration_System SHALL run without runtime logging errors
4. THE Migration_System SHALL produce log files in the expected locations
5. THE Migration_System SHALL maintain backward compatibility with existing functionality
