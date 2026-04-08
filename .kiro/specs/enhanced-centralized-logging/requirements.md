# Requirements Document

## Introduction

The RPlatform logging system needs enhancement to provide clean, centralized logging for all Raindrop Central plugins. The system should minimize console spam on live servers while maintaining comprehensive file-based logs with simple rotation (max 2 files). The logger must be easy to use, prevent recursion issues, and provide a clean separation between console and file output.

## Glossary

- **RPlatform**: The core platform library providing shared functionality for Raindrop Central plugins
- **Plugin Logger**: A logger instance specific to each plugin that uses RPlatform
- **Console Output**: Messages displayed in the Bukkit server console
- **File Output**: Messages written to plugin-specific log files
- **Log Rotation**: The process of managing log file size by creating backup files and overwriting old logs
- **Log Level**: The severity classification of log messages (SEVERE, WARNING, INFO, DEBUG)
- **Spam Prevention**: Mechanisms to reduce duplicate or excessive log messages

## Requirements

### Requirement 1

**User Story:** As a server administrator, I want minimal console output on live servers, so that the console remains clean and readable

#### Acceptance Criteria

1. THE RPlatform Logger SHALL write only SEVERE and WARNING level messages to the console by default
2. THE RPlatform Logger SHALL write all log levels (SEVERE, WARNING, INFO, DEBUG) to the log file
3. WHEN a plugin initializes its logger, THE RPlatform Logger SHALL allow configuration of console log level per plugin
4. THE RPlatform Logger SHALL provide a method to suppress console output entirely for a specific plugin
5. THE RPlatform Logger SHALL filter duplicate messages within a 5-second window to prevent console spam

### Requirement 2

**User Story:** As a plugin developer, I want simple log file management with automatic rotation, so that logs don't consume excessive disk space

#### Acceptance Criteria

1. THE RPlatform Logger SHALL maintain a maximum of 2 log files per plugin (current and backup)
2. WHEN the current log file exceeds 10MB, THE RPlatform Logger SHALL rename it to a backup file and create a new current log file
3. WHEN creating a backup file, THE RPlatform Logger SHALL overwrite any existing backup file
4. THE RPlatform Logger SHALL name log files using the pattern `{plugin-name}-latest.log` and `{plugin-name}-backup.log`
5. THE RPlatform Logger SHALL create log files in the plugin's data folder under a `logs` subdirectory

### Requirement 3

**User Story:** As a plugin developer, I want an easy-to-use logger API, so that I can add logging to my code with minimal effort

#### Acceptance Criteria

1. THE RPlatform Logger SHALL provide a static method `getLogger(JavaPlugin plugin)` that returns a configured logger instance
2. THE RPlatform Logger SHALL provide convenience methods `info()`, `warning()`, `severe()`, and `debug()` that accept String messages
3. THE RPlatform Logger SHALL provide overloaded methods that accept message formatting with parameters (e.g., `info(String format, Object... args)`)
4. THE RPlatform Logger SHALL provide methods that accept Throwable exceptions for stack trace logging
5. THE RPlatform Logger SHALL automatically include plugin name and timestamp in all log entries

### Requirement 4

**User Story:** As a plugin developer, I want the logger to prevent infinite recursion, so that my plugin doesn't crash with StackOverflowError

#### Acceptance Criteria

1. THE RPlatform Logger SHALL maintain a thread-local recursion counter that tracks logging depth
2. WHEN recursion depth exceeds 3 levels, THE RPlatform Logger SHALL write directly to the original output stream without invoking handlers
3. THE RPlatform Logger SHALL reset the recursion counter after each top-level logging operation completes
4. THE RPlatform Logger SHALL catch all exceptions during logging operations and prevent them from propagating to the caller
5. WHEN a logging exception occurs, THE RPlatform Logger SHALL write an error message to the original System.err stream

### Requirement 5

**User Story:** As a plugin developer, I want the logger to handle System.out and System.err redirects safely, so that existing code continues to work

#### Acceptance Criteria

1. THE RPlatform Logger SHALL redirect System.out to the logging system at INFO level
2. THE RPlatform Logger SHALL redirect System.err to the logging system at SEVERE level
3. WHEN System.out or System.err is used, THE RPlatform Logger SHALL apply recursion guards to prevent loops
4. THE RPlatform Logger SHALL maintain references to original System.out and System.err for emergency fallback
5. THE RPlatform Logger SHALL provide a method to restore original System.out and System.err during shutdown

### Requirement 6

**User Story:** As a server administrator, I want clean, readable log file formatting, so that I can easily diagnose issues

#### Acceptance Criteria

1. THE RPlatform Logger SHALL format log entries with the pattern `[HH:mm:ss LEVEL] [PluginName] Message`
2. THE RPlatform Logger SHALL write stack traces on separate lines with proper indentation
3. THE RPlatform Logger SHALL include thread name in log entries when the logging thread is not the main server thread
4. THE RPlatform Logger SHALL use UTF-8 encoding for all log files
5. THE RPlatform Logger SHALL flush log buffers every 5 seconds to ensure recent logs are written to disk

### Requirement 7

**User Story:** As a plugin developer, I want the logger to initialize automatically, so that I don't need complex setup code

#### Acceptance Criteria

1. WHEN a plugin calls `getLogger()` for the first time, THE RPlatform Logger SHALL automatically initialize the logging system
2. THE RPlatform Logger SHALL create the logs directory if it does not exist
3. THE RPlatform Logger SHALL handle initialization failures gracefully and fall back to standard Java logging
4. THE RPlatform Logger SHALL register a shutdown hook to flush and close log files when the plugin disables
5. THE RPlatform Logger SHALL support multiple plugins using the same logging system without conflicts
