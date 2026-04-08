# Design Document

## Overview

This design creates a clean, centralized logging system for RPlatform that minimizes console spam while maintaining comprehensive file logs. The system uses a simple 2-file rotation strategy, provides an easy-to-use API, and includes robust recursion protection. Each plugin gets its own logger instance with independent configuration while sharing the underlying infrastructure.

## Architecture

### High-Level Design

```
┌─────────────────────────────────────────────────────┐
│                   Plugin Code                        │
│  RLogger.getLogger(plugin).info("message")          │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│              RLogger (Facade)                        │
│  - getLogger(plugin) → PluginLogger                 │
│  - Manages plugin-specific loggers                  │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│           PluginLogger (Per-Plugin)                  │
│  - info(), warning(), severe(), debug()             │
│  - Recursion guard                                  │
│  - Delegates to handlers                            │
└──────────────────┬────────────────┬─────────────────┘
                   │                │
         ┌─────────┴────────┐      │
         ▼                  ▼       ▼
┌──────────────────┐ ┌──────────────────┐
│  FileHandler     │ │ ConsoleHandler   │
│  - 2-file rotate │ │ - Minimal output │
│  - All levels    │ │ - WARN+ only     │
└──────────────────┘ └──────────────────┘
```

### Key Design Decisions

1. **Facade Pattern**: `RLogger` provides static access, delegates to plugin-specific instances
2. **Per-Plugin Isolation**: Each plugin gets its own logger, file handlers, and configuration
3. **Lazy Initialization**: Loggers initialize on first use, no explicit setup required
4. **Thread-Local Guards**: Recursion protection per thread, no global locks
5. **Simple Rotation**: 2 files max (current + backup), overwrite old backup

## Components and Interfaces

### 1. RLogger (Main Facade)

**Purpose:** Static entry point for all logging operations

**Public API:**
```java
public final class RLogger {
    // Get or create logger for a plugin
    public static PluginLogger getLogger(JavaPlugin plugin);
    
    // Global configuration
    public static void setGlobalConsoleLevel(Level level);
    public static void shutdown();
    
    // Diagnostics
    public static boolean isInitialized(JavaPlugin plugin);
    public static String getLogFilePath(JavaPlugin plugin);
}
```

**Internal State:**
```java
private static final Map<String, PluginLogger> PLUGIN_LOGGERS = new ConcurrentHashMap<>();
private static final PrintStream ORIGINAL_OUT = System.out;
private static final PrintStream ORIGINAL_ERR = System.err;
private static volatile boolean SYSTEM_STREAMS_REDIRECTED = false;
```

### 2. PluginLogger (Per-Plugin Instance)

**Purpose:** Provides logging methods for a specific plugin with recursion protection

**Public API:**
```java
public final class PluginLogger {
    // Basic logging
    public void info(String message);
    public void warning(String message);
    public void severe(String message);
    public void debug(String message);
    
    // Formatted logging
    public void info(String format, Object... args);
    public void warning(String format, Object... args);
    public void severe(String format, Object... args);
    public void debug(String format, Object... args);
    
    // Exception logging
    public void severe(String message, Throwable throwable);
    public void warning(String message, Throwable throwable);
    
    // Configuration
    public void setConsoleLevel(Level level);
    public void setFileLevel(Level level);
    public void setConsoleEnabled(boolean enabled);
    
    // Lifecycle
    public void flush();
    public void close();
}
```

**Internal State:**
```java
private final JavaPlugin plugin;
private final Logger javaLogger;
private final RotatingFileHandler fileHandler;
private final FilteredConsoleHandler consoleHandler;
private final ThreadLocal<Integer> recursionDepth = ThreadLocal.withInitial(() -> 0);
private final AtomicBoolean emergencyMode = new AtomicBoolean(false);
private static final int MAX_RECURSION_DEPTH = 3;
```

### 3. RotatingFileHandler

**Purpose:** Manages 2-file log rotation with automatic backup

**Implementation:**
```java
class RotatingFileHandler extends Handler {
    private final File currentFile;    // {plugin}-latest.log
    private final File backupFile;     // {plugin}-backup.log
    private final long maxFileSize;    // 10MB
    private FileOutputStream outputStream;
    private long currentSize;
    
    @Override
    public void publish(LogRecord record) {
        if (currentSize >= maxFileSize) {
            rotate();
        }
        writeToFile(record);
    }
    
    private void rotate() {
        close();
        if (backupFile.exists()) {
            backupFile.delete();
        }
        currentFile.renameTo(backupFile);
        openNewFile();
    }
}
```

**File Naming:**
- Current: `plugins/{PluginName}/logs/{pluginname}-latest.log`
- Backup: `plugins/{PluginName}/logs/{pluginname}-backup.log`

### 4. FilteredConsoleHandler

**Purpose:** Writes to console with level filtering and spam prevention

**Implementation:**
```java
class FilteredConsoleHandler extends Handler {
    private final PrintStream originalOut;
    private final Map<String, Long> lastSeen = new ConcurrentHashMap<>();
    private static final long DUPLICATE_WINDOW_MS = 5000;
    
    @Override
    public void publish(LogRecord record) {
        // Check level filter
        if (record.getLevel().intValue() < getLevel().intValue()) {
            return;
        }
        
        // Check for duplicates
        String key = record.getMessage();
        Long lastTime = lastSeen.get(key);
        long now = System.currentTimeMillis();
        
        if (lastTime != null && (now - lastTime) < DUPLICATE_WINDOW_MS) {
            return; // Skip duplicate
        }
        
        lastSeen.put(key, now);
        originalOut.println(formatMessage(record));
    }
}
```

### 5. RecursionGuard (Mixin)

**Purpose:** Prevent infinite recursion in logging calls

**Implementation:**
```java
// In PluginLogger
private boolean enterLogging() {
    if (emergencyMode.get()) {
        return false;
    }
    
    int depth = recursionDepth.get();
    if (depth >= MAX_RECURSION_DEPTH) {
        activateEmergencyMode("Recursion depth exceeded");
        return false;
    }
    
    recursionDepth.set(depth + 1);
    return true;
}

private void exitLogging() {
    int depth = recursionDepth.get();
    if (depth > 0) {
        recursionDepth.set(depth - 1);
    }
}

private void activateEmergencyMode(String reason) {
    if (emergencyMode.compareAndSet(false, true)) {
        ORIGINAL_ERR.println("[RLogger EMERGENCY] " + plugin.getName() + ": " + reason);
    }
}

// Wrap all logging methods
public void info(String message) {
    if (!enterLogging()) {
        ORIGINAL_OUT.println("[" + plugin.getName() + "] " + message);
        return;
    }
    try {
        javaLogger.info(message);
    } catch (Exception e) {
        activateEmergencyMode("Exception during logging: " + e.getMessage());
        ORIGINAL_OUT.println("[" + plugin.getName() + "] " + message);
    } finally {
        exitLogging();
    }
}
```

### 6. System Stream Redirection

**Purpose:** Safely redirect System.out/err to logging system

**Implementation:**
```java
class SafeLoggingPrintStream extends PrintStream {
    private final PluginLogger logger;
    private final Level level;
    private final PrintStream original;
    
    @Override
    public void println(String x) {
        try {
            // Use logger with recursion protection
            if (level == Level.INFO) {
                logger.info(x);
            } else {
                logger.severe(x);
            }
        } catch (Exception e) {
            // Fallback to original
            original.println(x);
        }
    }
}

// In RLogger initialization
public static synchronized void redirectSystemStreams(PluginLogger defaultLogger) {
    if (!SYSTEM_STREAMS_REDIRECTED) {
        System.setOut(new SafeLoggingPrintStream(defaultLogger, Level.INFO, ORIGINAL_OUT));
        System.setErr(new SafeLoggingPrintStream(defaultLogger, Level.SEVERE, ORIGINAL_ERR));
        SYSTEM_STREAMS_REDIRECTED = true;
    }
}
```

## Data Models

### LogRecord Format

```
[HH:mm:ss LEVEL] [PluginName] Message
[HH:mm:ss LEVEL] [PluginName] [ThreadName] Message  // if not main thread
```

**Example:**
```
[14:32:15 INFO] [RDQ] Player joined: Steve
[14:32:16 WARNING] [RDQ] [Async-Worker-1] Database connection slow
[14:32:17 SEVERE] [RDQ] Failed to load rank data
java.sql.SQLException: Connection timeout
    at com.raindropcentral.rdq.database.RankRepository.load(RankRepository.java:45)
    at com.raindropcentral.rdq.RDQ.onEnable(RDQ.java:23)
```

### Configuration Model

```java
class LoggerConfig {
    Level consoleLevel = Level.WARNING;  // Default: WARN and above to console
    Level fileLevel = Level.ALL;         // Default: Everything to file
    boolean consoleEnabled = true;
    long maxFileSize = 10 * 1024 * 1024; // 10MB
    int maxFiles = 2;                    // Current + backup
    long duplicateWindowMs = 5000;       // 5 seconds
}
```

## Error Handling

### Initialization Failures

**Scenario:** Cannot create log directory or file
**Response:**
1. Log error to System.err
2. Fall back to standard Java logger
3. Continue plugin loading (don't fail)
4. Set emergency mode flag

### File Write Failures

**Scenario:** Disk full or permission denied
**Response:**
1. Catch IOException
2. Activate emergency mode
3. Write to console only
4. Log error once to System.err

### Recursion Detection

**Scenario:** Logging triggers more logging
**Response:**
1. Increment recursion counter
2. If > MAX_DEPTH, activate emergency mode
3. Write directly to original streams
4. Prevent StackOverflowError

### Handler Exceptions

**Scenario:** Handler throws exception during publish
**Response:**
1. Catch in PluginLogger
2. Activate emergency mode
3. Write to original stream
4. Continue execution

## Testing Strategy

### Unit Tests

1. **Test PluginLogger API**
   - Verify all logging methods work
   - Test formatted logging with parameters
   - Test exception logging with stack traces

2. **Test Recursion Guard**
   - Verify counter increments/decrements
   - Test emergency mode activation
   - Test thread isolation

3. **Test RotatingFileHandler**
   - Verify file creation
   - Test rotation at max size
   - Verify backup file overwrite
   - Test file naming

4. **Test FilteredConsoleHandler**
   - Verify level filtering
   - Test duplicate message suppression
   - Verify time window behavior

### Integration Tests

1. **Test Multi-Plugin Scenario**
   - Create loggers for multiple plugins
   - Verify independent log files
   - Verify no cross-contamination

2. **Test System Stream Redirection**
   - Call System.out.println()
   - Verify appears in log file
   - Verify no recursion errors

3. **Test Full Logging Pipeline**
   - Initialize logger
   - Log at various levels
   - Verify console output (WARN+)
   - Verify file output (ALL)
   - Check file rotation

### Manual Testing

1. Deploy to test server with RDQ
2. Verify clean console output
3. Check log files in plugins/RDQ/logs/
4. Verify rotation after 10MB
5. Test with multiple plugins
6. Verify no StackOverflowError

## Performance Considerations

### Memory

- ThreadLocal overhead: ~100 bytes per thread per logger
- Log file buffers: 8KB per handler
- Duplicate detection map: Bounded by time window (auto-cleanup)

### CPU

- Recursion check: Single integer comparison
- Level filtering: Integer comparison
- Duplicate detection: HashMap lookup (O(1))
- File rotation: Rare operation (every 10MB)

### I/O

- Buffered file writes (8KB buffer)
- Async flush every 5 seconds
- Rotation is synchronous but infrequent

## Migration Path

### From Current CentralLogger

1. Keep CentralLogger for backward compatibility
2. Mark as deprecated
3. Internally delegate to RLogger
4. Provide migration guide

### For Existing Plugins

**Before:**
```java
Logger logger = Logger.getLogger(getClass().getName());
logger.info("Message");
```

**After:**
```java
PluginLogger logger = RLogger.getLogger(this);
logger.info("Message");
```

## Implementation Notes

### Thread Safety

- ConcurrentHashMap for plugin logger registry
- ThreadLocal for recursion counters
- AtomicBoolean for emergency mode
- Synchronized file rotation

### Initialization Order

1. First `getLogger()` call creates PluginLogger
2. PluginLogger creates handlers
3. Handlers create log files
4. System streams redirected (once globally)

### Shutdown Sequence

1. Plugin disable hook triggered
2. Flush all handlers
3. Close file handlers
4. Remove from registry
5. Restore System streams (if last plugin)

## Diagrams

### Component Interaction

```
Plugin Code
    │
    ├─→ RLogger.getLogger(plugin)
    │       │
    │       ├─→ Create PluginLogger (if new)
    │       │       │
    │       │       ├─→ Create RotatingFileHandler
    │       │       └─→ Create FilteredConsoleHandler
    │       │
    │       └─→ Return PluginLogger
    │
    └─→ logger.info("message")
            │
            ├─→ enterLogging() [recursion guard]
            │
            ├─→ javaLogger.log()
            │       │
            │       ├─→ RotatingFileHandler.publish()
            │       │       └─→ Write to file
            │       │
            │       └─→ FilteredConsoleHandler.publish()
            │               └─→ Write to console (if level matches)
            │
            └─→ exitLogging()
```

### File Rotation Flow

```
Log Message
    │
    ▼
Check File Size
    │
    ├─→ < 10MB: Write to current file
    │
    └─→ >= 10MB: Rotate
            │
            ├─→ Close current file
            ├─→ Delete backup (if exists)
            ├─→ Rename current → backup
            ├─→ Create new current file
            └─→ Write message
```
