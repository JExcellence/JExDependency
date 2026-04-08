# Requirements Document

## Introduction

The CentralLogger system in RPlatform is experiencing a StackOverflowError due to infinite recursion in the logging pipeline. When LoggingPrintStream attempts to log messages, it triggers log handlers that write back to the same stream, creating a circular dependency. This critical issue prevents plugins (like RDQ) from loading properly and must be resolved to restore system stability.

## Glossary

- **CentralLogger**: The centralized logging system for Raindrop Central plugins that manages file and console output
- **LoggingPrintStream**: A custom PrintStream implementation that redirects System.out and System.err to the logging system
- **LogHandler**: A java.util.logging.Handler that processes and outputs log records
- **Recursion Guard**: A mechanism to detect and prevent circular method calls in the logging pipeline
- **Emergency Mode**: A fallback state where logging bypasses normal handlers to prevent system failure

## Requirements

### Requirement 1

**User Story:** As a plugin developer, I want the logging system to handle recursive logging attempts gracefully, so that my plugin can start without StackOverflowErrors

#### Acceptance Criteria

1. WHEN LoggingPrintStream writes a log message, THE CentralLogger SHALL detect if the current thread is already processing a log message
2. IF a recursive logging attempt is detected, THEN THE CentralLogger SHALL bypass normal log handlers and write directly to the original stream
3. THE CentralLogger SHALL maintain a thread-local recursion counter that increments before logging and decrements after completion
4. WHEN the recursion depth exceeds 3 levels, THE CentralLogger SHALL activate emergency mode and use direct stream output
5. THE CentralLogger SHALL reset the recursion counter after each top-level logging operation completes

### Requirement 2

**User Story:** As a system administrator, I want the logging system to fail gracefully when errors occur, so that the server remains operational even if logging breaks

#### Acceptance Criteria

1. WHEN any logging operation throws an exception, THE CentralLogger SHALL catch the exception and prevent it from propagating
2. IF emergency mode is activated, THE CentralLogger SHALL log a warning message to the original output streams
3. WHILE emergency mode is active, THE CentralLogger SHALL write all log messages directly to original System.out and System.err
4. THE CentralLogger SHALL provide a method to check if emergency mode is active
5. WHEN emergency mode is triggered, THE CentralLogger SHALL record the triggering exception for diagnostic purposes

### Requirement 3

**User Story:** As a plugin developer, I want LoggingPrintStream to avoid triggering log handlers that write back to System streams, so that circular dependencies are eliminated

#### Acceptance Criteria

1. WHEN LoggingPrintStream logs a message, THE CentralLogger SHALL check the recursion guard before invoking any handlers
2. IF the recursion guard indicates a recursive call, THEN THE LoggingPrintStream SHALL write directly to the original stream without logging
3. THE LoggingPrintStream SHALL wrap all logging operations in try-catch blocks to prevent exception propagation
4. WHEN writing to the original stream, THE LoggingPrintStream SHALL not invoke any Logger methods
5. THE LoggingPrintStream SHALL maintain a reference to the original PrintStream for direct output

### Requirement 4

**User Story:** As a plugin developer, I want the logging system to be testable and debuggable, so that I can verify it works correctly and diagnose issues

#### Acceptance Criteria

1. THE CentralLogger SHALL provide a method to retrieve the current recursion depth for the calling thread
2. THE CentralLogger SHALL provide a method to check if emergency mode has been activated
3. THE CentralLogger SHALL log diagnostic information when emergency mode is triggered
4. WHEN debug mode is enabled, THE CentralLogger SHALL log recursion depth changes to help diagnose issues
5. THE CentralLogger SHALL expose metrics about recursion prevention activations
