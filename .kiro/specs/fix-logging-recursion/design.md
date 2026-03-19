# Design Document

## Overview

This design addresses the StackOverflowError in CentralLogger by implementing a thread-local recursion guard that prevents circular logging calls. The solution uses a ThreadLocal counter to track recursion depth and provides an emergency fallback mode that bypasses the logging system when recursion is detected.

## Architecture

### Current Problem

The current architecture has a circular dependency:
1. `LoggingPrintStream.println()` → calls `logger.log()`
2. `logger.log()` → triggers handlers (FileHandler, ConsoleHandler)
3. Handlers write to streams → may trigger `LoggingPrintStream` again
4. Loop continues until StackOverflowError

### Proposed Solution

Implement a recursion guard using ThreadLocal storage:
- Track recursion depth per thread
- Bypass logging handlers when recursion is detected
- Fall back to direct stream output in emergency scenarios
- Maintain thread safety without locks

## Components and Interfaces

### 1. Recursion Guard

**Location:** `CentralLogger` class

**Fields:**
```java
private static final ThreadLocal<Integer> RECURSION_DEPTH = ThreadLocal.withInitial(() -> 0);
private static final int MAX_RECURSION_DEPTH = 3;
private static final AtomicBoolean EMERGENCY_MODE = new AtomicBoolean(false);
```

**Methods:**
```java
private static boolean enterLogging() {
    int depth = RECURSION_DEPTH.get();
    if (depth >= MAX_RECURSION_DEPTH) {
        activateEmergencyMode("Max recursion depth exceeded");
        return false;
    }
    RECURSION_DEPTH.set(depth + 1);
    return true;
}

private static void exitLogging() {
    int depth = RECURSION_DEPTH.get();
    if (depth > 0) {
        RECURSION_DEPTH.set(depth - 1);
    }
}

private static void activateEmergencyMode(String reason) {
    if (EMERGENCY_MODE.compareAndSet(false, true)) {
        safeErr("[CentralLogger EMERGENCY] Activated emergency mode: " + reason);
    }
}
```

### 2. Modified LoggingPrintStream

**Changes:**
- Check recursion guard before logging
- Use direct stream output when guard fails
- Wrap all operations in try-catch

**Implementation:**
```java
private static final class LoggingPrintStream extends PrintStream {
    private final Logger logger;
    private final Level level;
    private final PrintStream original;

    @Override
    public void println(String x) {
        try {
            if (enterLogging()) {
                try {
                    if (logger != null && !EMERGENCY_MODE.get()) {
                        logger.log(level, x);
                    } else {
                        original.println(x);
                    }
                } finally {
                    exitLogging();
                }
            } else {
                // Recursion detected, use direct output
                original.println(x);
            }
        } catch (Exception e) {
            // Last resort: direct output
            original.println(x);
        }
    }
    
    // Similar implementation for other print methods
}
```

### 3. Handler Configuration

**Changes:**
- Ensure handlers don't write to System.out/err
- Configure handlers to use original streams when needed
- Add filters to prevent handler loops

**FileHandler:** No changes needed (writes to file)

**ConsoleHandler:** Verify it uses original streams, not redirected ones

### 4. Emergency Mode

**Behavior:**
- Activated when recursion depth exceeds limit
- Activated when any logging exception occurs
- All logging bypasses handlers and uses direct stream output
- Remains active for the lifetime of the logger
- Logs diagnostic information about activation

**Methods:**
```java
public static boolean isEmergencyMode() {
    return EMERGENCY_MODE.get();
}

public static int getCurrentRecursionDepth() {
    return RECURSION_DEPTH.get();
}
```

## Data Models

### Thread-Local State

```java
ThreadLocal<Integer> RECURSION_DEPTH
- Scope: Per-thread
- Lifecycle: Managed automatically by ThreadLocal
- Initial value: 0
- Max value: MAX_RECURSION_DEPTH (3)
```

### Emergency Mode State

```java
AtomicBoolean EMERGENCY_MODE
- Scope: Global
- Lifecycle: Set once, never reset
- Thread-safe: Uses AtomicBoolean
- Initial value: false
```

## Error Handling

### Recursion Detection

**Scenario:** Logging call while already logging
**Response:** 
1. Increment recursion counter
2. If counter > MAX_RECURSION_DEPTH, activate emergency mode
3. Use direct stream output
4. Decrement counter in finally block

### Handler Exceptions

**Scenario:** Handler throws exception during publish
**Response:**
1. Catch exception in LoggingPrintStream
2. Activate emergency mode
3. Fall back to direct stream output
4. Log exception details to original error stream

### Initialization Failures

**Scenario:** Logger initialization fails
**Response:**
1. Catch exception in initialize()
2. Log error to original streams
3. Set INITIALIZED = false
4. Allow plugin to continue with standard logging

## Testing Strategy

### Unit Tests

1. **Test Recursion Guard**
   - Verify recursion counter increments/decrements correctly
   - Verify emergency mode activates at max depth
   - Verify thread isolation (different threads have independent counters)

2. **Test LoggingPrintStream**
   - Verify normal logging works
   - Verify recursion detection prevents loops
   - Verify fallback to direct output
   - Verify exception handling

3. **Test Emergency Mode**
   - Verify activation conditions
   - Verify one-time activation (doesn't toggle)
   - Verify logging behavior in emergency mode

### Integration Tests

1. **Test Full Logging Pipeline**
   - Initialize CentralLogger
   - Trigger System.out.println()
   - Verify no StackOverflowError
   - Verify message appears in log file

2. **Test Recursive Logging Scenario**
   - Create handler that logs during publish
   - Verify recursion guard prevents infinite loop
   - Verify emergency mode activates
   - Verify messages still output

3. **Test Plugin Loading**
   - Load RDQ plugin with fixed logger
   - Verify plugin enables successfully
   - Verify no StackOverflowError during startup

### Manual Testing

1. Build and deploy to test server
2. Start server and observe plugin loading
3. Verify RDQ loads without errors
4. Check log files for proper output
5. Verify console output is clean
6. Test various logging scenarios (info, warning, error)

## Implementation Notes

### Thread Safety

- Use ThreadLocal for per-thread state (no locks needed)
- Use AtomicBoolean for emergency mode (thread-safe updates)
- No shared mutable state between threads

### Performance

- ThreadLocal access is fast (no synchronization)
- Recursion check is a simple integer comparison
- Emergency mode check is a volatile read
- Minimal overhead in normal operation

### Backward Compatibility

- No API changes to public methods
- Existing logger usage continues to work
- Emergency mode is transparent to callers
- File and console output behavior unchanged

## Diagrams

### Logging Flow with Recursion Guard

```
┌─────────────────────────────────────────┐
│ Application calls System.out.println()  │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│ LoggingPrintStream.println()            │
│ - Check enterLogging()                  │
└──────────────────┬──────────────────────┘
                   │
         ┌─────────┴─────────┐
         │                   │
         ▼                   ▼
    [Allowed]           [Blocked]
         │                   │
         ▼                   ▼
┌─────────────────┐  ┌──────────────────┐
│ logger.log()    │  │ original.println()│
│ (normal path)   │  │ (direct output)  │
└────────┬────────┘  └──────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│ Handlers process (FileHandler, etc.)    │
│ - If handler tries to log again...      │
│ - Recursion guard blocks it             │
└─────────────────────────────────────────┘
```

### Emergency Mode Activation

```
┌─────────────────────────────────────────┐
│ Recursion depth > MAX or Exception      │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│ activateEmergencyMode()                 │
│ - Set EMERGENCY_MODE = true             │
│ - Log to original error stream          │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│ All future logging bypasses handlers    │
│ - Direct output to original streams     │
│ - No handler invocation                 │
│ - System remains stable                 │
└─────────────────────────────────────────┘
```
