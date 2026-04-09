# WAL Corruption Fix - Statistics Queue

## Problem

The statistics queue system was experiencing JSON parsing errors when loading the Write-Ahead Log (WAL) file on startup:

```
[WARN] Failed to parse WAL entry: java.io.EOFException: End of input at line 1 column 2 path $.
[WARN] Failed to parse WAL entry: java.lang.IllegalStateException: Expected BEGIN_OBJECT but was STRING at line 1 column 4 path $
[WARN] Failed to parse WAL entry: com.google.gson.stream.MalformedJsonException: Expected value at line 1 column 1 path $
```

These errors occurred when the `statistics-queue.wal` file contained:
- Incomplete JSON entries (truncated during write)
- Empty lines or whitespace
- Non-JSON content
- Corrupted data from improper shutdown

## Root Cause

The `applyWal()` method in `QueuePersistenceManager` was attempting to parse every line in the WAL file without proper validation, causing exceptions when encountering malformed entries.

## Solution

### 1. Enhanced WAL Entry Validation

Added robust validation in `applyWal()` method:

```java
// Skip empty or whitespace-only lines
if (line.isBlank()) {
    continue;
}

// Skip lines that are clearly not JSON objects
String trimmed = line.trim();
if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
    LOGGER.fine("Skipping malformed WAL entry at line " + lineNumber + ": not a JSON object");
    skipped++;
    continue;
}
```

### 2. Specific Exception Handling

Added specific handling for different types of JSON parsing errors:

```java
try {
    SerializableStatistic ser = gson.fromJson(line, SerializableStatistic.class);
    // ... process entry
} catch (com.google.gson.JsonSyntaxException e) {
    LOGGER.fine("Skipping malformed JSON at line " + lineNumber + ": " + e.getMessage());
    skipped++;
} catch (Exception e) {
    LOGGER.warning("Failed to parse WAL entry at line " + lineNumber + ": " + e.getMessage());
    skipped++;
}
```

### 3. WAL Cleanup on Startup

Added `cleanupCorruptedWal()` method that runs during `validateAndRepair()`:

- Reads all WAL entries
- Validates each entry by attempting to parse it
- Rewrites the WAL file with only valid entries
- Deletes the WAL file if no valid entries remain

```java
private void cleanupCorruptedWal() {
    // Read and validate all lines
    // Keep only valid JSON entries
    // Rewrite WAL file or delete if empty
}
```

### 4. Improved Logging

- Uses `LOGGER.fine()` for expected skips (reduces noise)
- Uses `LOGGER.warning()` for unexpected errors
- Provides summary of applied vs skipped entries
- Includes line numbers for debugging

## Benefits

1. **Graceful Recovery**: Server starts successfully even with corrupted WAL files
2. **Data Preservation**: Valid entries are preserved and applied
3. **Automatic Cleanup**: Corrupted entries are automatically removed
4. **Better Diagnostics**: Clear logging shows what was skipped and why
5. **No Data Loss**: Only truly corrupted entries are discarded

## Testing

Comprehensive test suite added in `QueuePersistenceManagerTest.java`:

- `testLoadValidWalEntries()` - Verifies valid entries load correctly
- `testSkipCorruptedWalEntries()` - Verifies corrupted entries are skipped gracefully
- `testWalCleanupRemovesCorruptedEntries()` - Verifies cleanup removes invalid entries
- `testCompletelyCorruptedWalIsDeleted()` - Verifies completely corrupted WAL is deleted
- `testEmptyWalFile()` - Verifies empty WAL is handled correctly
- `testWalWithOnlyWhitespace()` - Verifies whitespace-only WAL is handled
- `testPersistAndLoad()` - Verifies persist/load cycle works
- `testCapacityChecks()` - Verifies capacity management

All tests pass successfully.

## Files Modified

- `RCore/src/main/java/com/raindropcentral/core/service/statistics/queue/QueuePersistenceManager.java`
  - Enhanced `applyWal()` method with validation and better error handling
  - Added `cleanupCorruptedWal()` method for automatic WAL repair
  - Updated `validateAndRepair()` to call cleanup on startup

## Files Added

- `RCore/src/test/java/com/raindropcentral/core/service/statistics/queue/QueuePersistenceManagerTest.java`
  - Comprehensive test suite for WAL corruption handling
  - 8 test cases covering various corruption scenarios

## Build Verification

```bash
./gradlew :RCore:build -x test
# BUILD SUCCESSFUL

./gradlew :RCore:test --tests QueuePersistenceManagerTest
# BUILD SUCCESSFUL - 8 tests passed

./gradlew :RCore:javadoc
# BUILD SUCCESSFUL - No warnings
```

## Related Issues

This fix resolves the WAL parsing errors that were occurring on server startup when backup/config files from previous sessions contained corrupted data.

## Future Improvements

Consider:
- Adding WAL file size limits to prevent unbounded growth
- Implementing WAL rotation (e.g., `.wal.1`, `.wal.2`)
- Adding checksums to WAL entries for integrity verification
- Periodic WAL compaction during runtime
- Atomic write operations to prevent partial writes

