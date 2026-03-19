# Implementation Plan

- [x] 1. Create core RLogger facade class





  - Create `RLogger` class in `com.raindropcentral.rplatform.logging` package
  - Implement `getLogger(JavaPlugin plugin)` method with plugin logger registry
  - Add static fields for original System.out/err references
  - Implement `setGlobalConsoleLevel(Level level)` method
  - Add `shutdown()` method to close all plugin loggers
  - Add diagnostic methods `isInitialized()` and `getLogFilePath()`
  - _Requirements: 3.1, 7.1, 7.5_

- [x] 2. Implement PluginLogger class with recursion protection






  - Create `PluginLogger` class with plugin reference and Java Logger instance
  - Add ThreadLocal recursion depth counter and emergency mode flag
  - Implement `enterLogging()` and `exitLogging()` methods for recursion guard
  - Implement `activateEmergencyMode(String reason)` method
  - Add basic logging methods: `info()`, `warning()`, `severe()`, `debug()`
  - Wrap all logging methods with recursion guard (try-finally blocks)
  - Add fallback to original streams when recursion detected
  - _Requirements: 3.1, 3.2, 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 3. Add formatted logging and exception handling to PluginLogger





  - Implement overloaded methods accepting format strings and varargs: `info(String format, Object... args)`
  - Implement exception logging methods: `severe(String message, Throwable throwable)`
  - Add `warning(String message, Throwable throwable)` method
  - Use `String.format()` for parameter substitution
  - Ensure all methods use recursion guard
  - _Requirements: 3.3, 3.4_

- [x] 4. Create RotatingFileHandler for 2-file log rotation





  - Create `RotatingFileHandler` class extending `java.util.logging.Handler`
  - Implement constructor that creates log directory and files
  - Add fields for current file, backup file, max size (10MB), and current size tracking
  - Implement `publish(LogRecord record)` method with size checking
  - Implement `rotate()` method that deletes backup, renames current to backup, creates new current
  - Use file naming pattern: `{pluginname}-latest.log` and `{pluginname}-backup.log`
  - Add proper file closing and flushing in `close()` method
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 6.4_

- [x] 5. Create FilteredConsoleHandler with spam prevention





  - Create `FilteredConsoleHandler` class extending `java.util.logging.Handler`
  - Add reference to original System.out PrintStream
  - Implement level filtering in `publish()` method (default: WARNING and above)
  - Add duplicate message detection using ConcurrentHashMap with timestamps
  - Implement 5-second duplicate window check
  - Add periodic cleanup of old entries from duplicate detection map
  - Write to original System.out (not redirected stream)
  - _Requirements: 1.1, 1.5, 6.1_

- [x] 6. Implement custom log formatting





  - Create `RLogFormatter` class extending `java.util.logging.Formatter`
  - Implement `format(LogRecord record)` method with pattern: `[HH:mm:ss LEVEL] [PluginName] Message`
  - Add thread name to format when not on main thread: `[HH:mm:ss LEVEL] [PluginName] [ThreadName] Message`
  - Format stack traces with proper indentation on separate lines
  - Apply formatter to both RotatingFileHandler and FilteredConsoleHandler
  - _Requirements: 3.5, 6.1, 6.2, 6.3_

- [x] 7. Add configuration methods to PluginLogger





  - Implement `setConsoleLevel(Level level)` method
  - Implement `setFileLevel(Level level)` method
  - Implement `setConsoleEnabled(boolean enabled)` method
  - Add `flush()` method that flushes all handlers
  - Add `close()` method that closes handlers and removes from registry
  - _Requirements: 1.3, 1.4_

- [x] 8. Implement System stream redirection





  - Create `SafeLoggingPrintStream` class extending `PrintStream`
  - Override `println(String x)` and `println(Object x)` methods
  - Add recursion protection in print methods
  - Implement fallback to original stream on errors
  - Add `redirectSystemStreams(PluginLogger defaultLogger)` method in RLogger
  - Redirect System.out to INFO level and System.err to SEVERE level
  - Ensure redirection happens only once globally
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 9. Add automatic initialization and lifecycle management





  - Implement lazy initialization in `getLogger()` method
  - Create log directory if it doesn't exist
  - Add exception handling for initialization failures with fallback to standard logging
  - Register shutdown hook in PluginLogger to flush and close on plugin disable
  - Implement cleanup in RLogger.shutdown() to restore System streams
  - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [x] 10. Add UTF-8 encoding and periodic flushing





  - Set UTF-8 encoding on RotatingFileHandler
  - Set UTF-8 encoding on FilteredConsoleHandler
  - Implement scheduled flush task that runs every 5 seconds
  - Use Bukkit scheduler for flush task
  - Cancel flush task on logger close
  - _Requirements: 6.4, 6.5_

- [ ]* 11. Create unit tests for PluginLogger
  - Test all logging methods (info, warning, severe, debug)
  - Test formatted logging with parameters
  - Test exception logging with stack traces
  - Test recursion guard activation
  - Test emergency mode behavior
  - Test thread isolation of recursion counters
  - _Requirements: 3.2, 3.3, 3.4, 4.1, 4.2, 4.3_

- [ ]* 12. Create unit tests for RotatingFileHandler
  - Test file creation in logs directory
  - Test log writing to current file
  - Test rotation when file exceeds 10MB
  - Test backup file overwrite
  - Test file naming pattern
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [ ]* 13. Create unit tests for FilteredConsoleHandler
  - Test level filtering (only WARN+ to console)
  - Test duplicate message suppression
  - Test 5-second time window behavior
  - Test cleanup of old duplicate entries
  - _Requirements: 1.1, 1.5_

- [ ]* 14. Create integration tests for multi-plugin scenario
  - Create loggers for multiple mock plugins
  - Verify independent log files created
  - Verify no cross-contamination between plugin logs
  - Test concurrent logging from multiple plugins
  - _Requirements: 7.5_

- [ ]* 15. Create integration tests for System stream redirection
  - Test System.out.println() appears in log file
  - Test System.err.println() appears in log file at SEVERE level
  - Verify no recursion errors occur
  - Test fallback to original streams on errors
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [ ] 16. Test with RDQ plugin on live server
  - Build RPlatform with new RLogger
  - Update RDQ to use RLogger.getLogger(this)
  - Deploy to test server and start
  - Verify clean console output (only WARN+ messages)
  - Check log files in plugins/RDQ/logs/ directory
  - Verify log rotation after generating 10MB of logs
  - Verify no StackOverflowError during startup
  - Test with multiple plugins simultaneously
  - _Requirements: 1.1, 1.2, 2.1, 2.2, 4.1, 7.1_

- [ ]* 17. Create migration guide and deprecate CentralLogger
  - Document migration steps from CentralLogger to RLogger
  - Add deprecation annotations to CentralLogger
  - Update CentralLogger to internally delegate to RLogger for backward compatibility
  - Create example code snippets for common use cases
  - _Requirements: 3.1, 3.2, 3.3_
