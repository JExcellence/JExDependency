# Design Document

## Overview

This document outlines the design for migrating all existing logging in RPlatform and RDQ to the new centralized logging system. The migration will be performed systematically to ensure all files are checked and updated correctly.

## Architecture

### Migration Strategy

The migration follows a phased approach:

1. **Discovery Phase**: Identify all files with logging usage
2. **Analysis Phase**: Categorize logging patterns and determine required changes
3. **Refactoring Phase**: Apply systematic transformations to each file
4. **Verification Phase**: Compile and test to ensure correctness

### Logging Patterns

#### Pattern 1: Static Logger Fields
```java
// OLD
private static final Logger LOGGER = Logger.getLogger(ClassName.class.getName());

// NEW
private final PluginLogger logger;

public ClassName(JavaPlugin plugin) {
    this.logger = CentralLogger.getLogger(plugin);
}
```

#### Pattern 2: Incorrect CentralLogger Usage
```java
// OLD
CentralLogger.getLogger(ClassName.class.getName())
CentralLogger.getLogger(ClassName.class)
CentralLogger.getLogger("PluginName")

// NEW
CentralLogger.getLogger(pluginInstance)
```

#### Pattern 3: Logger Method Calls
```java
// OLD
logger.log(Level.INFO, "message");
logger.log(Level.WARNING, "message", exception);

// NEW
logger.info("message");
logger.warning("message", exception);
```

#### Pattern 4: PlatformLogger References
```java
// OLD
import com.raindropcentral.rplatform.logging.PlatformLogger;
private final PlatformLogger logger;

// NEW
import com.raindropcentral.rplatform.logging.PluginLogger;
private final PluginLogger logger;
```

## Components and Interfaces

### File Categories

Files are categorized based on their logging needs:

1. **Main Plugin Classes**: Have direct JavaPlugin access
   - RPlatform.java
   - RDQ.java
   - RDQPremiumImpl.java

2. **Service Classes**: Receive plugin via constructor
   - Requirement implementations
   - Reward implementations
   - View classes
   - Managers and services

3. **Utility Classes**: Receive PluginLogger via constructor
   - Bridges
   - Factories
   - Parsers
   - Validators

4. **Static Utility Classes**: Use passed PluginLogger parameter
   - Static helper methods
   - Utility functions

### Logger Propagation Strategy

```
JavaPlugin (root)
    ↓
CentralLogger.getLogger(plugin) → PluginLogger
    ↓
Constructor Injection
    ↓
Service/Component Classes
    ↓
Nested Components (via constructor)
```

## Data Models

### Migration Tracking

Each file migration tracks:
- File path
- Original logging pattern
- Required changes
- Migration status (pending/in-progress/complete)
- Compilation status

### Change Types

1. **Import Changes**: Update import statements
2. **Field Changes**: Modify logger field declarations
3. **Constructor Changes**: Add logger parameters
4. **Method Call Changes**: Update logging method calls
5. **Instantiation Changes**: Pass logger to constructors

## Error Handling

### Compilation Errors

- Track all compilation errors after each file migration
- Fix errors immediately before proceeding
- Ensure no new errors are introduced

### Runtime Errors

- Verify logger initialization doesn't cause NullPointerException
- Ensure plugin instance is available when needed
- Handle cases where plugin might not be initialized

## Testing Strategy

### Compilation Testing

After each major change:
1. Run `gradlew :RPlatform:compileJava`
2. Run `gradlew :RDQ:rdq-common:compileJava`
3. Run `gradlew :RDQ:rdq-premium:compileJava`

### Integration Testing

1. Verify log files are created in correct locations
2. Verify log messages are formatted correctly
3. Verify no recursion issues occur
4. Verify periodic flushing works

## Implementation Phases

### Phase 1: Core Platform Classes
- RPlatform.java
- Remove PlatformLogger class
- Update main plugin initialization

### Phase 2: RPlatform Service Classes
- Requirement implementations
- Reward implementations
- View classes
- Bridges and integrations

### Phase 3: RDQ Common Module
- RDQ.java main class
- Perk system classes
- Rank system classes
- View classes
- Listeners

### Phase 4: RDQ Premium Module
- RDQPremiumImpl.java
- Premium-specific features

### Phase 5: Verification
- Full build test
- Runtime verification
- Log file inspection

## Migration Checklist Per File

For each Java file:
- [ ] Identify current logging pattern
- [ ] Determine if JavaPlugin access exists
- [ ] Update imports
- [ ] Update field declarations
- [ ] Update constructor (add logger parameter if needed)
- [ ] Update all instantiation sites
- [ ] Update logging method calls
- [ ] Verify compilation
- [ ] Update JavaDoc if needed
