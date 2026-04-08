# Implementation Plan

- [ ] 1. Implement recursion guard infrastructure in CentralLogger
  - Add `enterLogging()` method that checks and increments recursion depth counter
  - Add `exitLogging()` method that decrements recursion depth counter
  - Add `activateEmergencyMode(String reason)` method that sets emergency flag and logs diagnostic info
  - Add public methods `isEmergencyMode()` and `getCurrentRecursionDepth()` for diagnostics
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.2, 2.4, 4.1, 4.2_

- [ ] 2. Update LoggingPrintStream to use recursion guard
  - Modify `println(String x)` to call `enterLogging()` before logging and `exitLogging()` in finally block
  - Add fallback to `original.println()` when `enterLogging()` returns false
  - Wrap logging operations in try-catch to activate emergency mode on exceptions
  - Modify `println(Object x)` with same recursion guard pattern
  - Override `write()` methods to use recursion guard for byte-level operations
  - _Requirements: 1.1, 1.2, 2.1, 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 3. Add emergency mode handling throughout CentralLogger
  - Update `LoggingPrintStream` to check `EMERGENCY_MODE` flag before invoking logger
  - Modify initialization to catch and handle exceptions without propagating them
  - Add diagnostic logging when emergency mode is activated
  - Update `flush()` and `shutdown()` methods to handle emergency mode gracefully
  - _Requirements: 2.1, 2.2, 2.3, 2.5, 4.3_

- [ ] 4. Verify handler configuration prevents circular dependencies
  - Review `setupConsoleHandler()` to ensure it doesn't write to redirected System.out/err
  - Verify `FileHandler` configuration writes only to files
  - Add comments documenting why handlers must not use System streams
  - _Requirements: 3.1, 3.4_

- [ ]* 5. Add debug logging for recursion tracking
  - Add debug log statements in `enterLogging()` and `exitLogging()` when DEBUG_MODE is enabled
  - Log recursion depth changes to help diagnose issues
  - Add metrics counter for recursion prevention activations
  - _Requirements: 4.4, 4.5_

- [ ]* 6. Create unit tests for recursion guard
  - Test that recursion counter increments and decrements correctly
  - Test that emergency mode activates when max depth is exceeded
  - Test thread isolation (different threads have independent counters)
  - Test that `enterLogging()` returns false after max depth
  - _Requirements: 1.1, 1.3, 1.4, 1.5_

- [ ]* 7. Create integration tests for logging pipeline
  - Test full logging pipeline from System.out.println() to file output
  - Test that recursive logging scenario activates emergency mode
  - Test that messages still output in emergency mode
  - Verify no StackOverflowError occurs during recursive logging
  - _Requirements: 2.1, 2.2, 2.3, 3.1, 3.2_

- [ ] 8. Test with RDQ plugin loading
  - Build RPlatform with fixes
  - Build RDQ plugin that depends on RPlatform
  - Deploy to test server and start
  - Verify RDQ loads without StackOverflowError
  - Check log files for proper output and no emergency mode activation
  - _Requirements: 1.1, 2.1, 2.3_
