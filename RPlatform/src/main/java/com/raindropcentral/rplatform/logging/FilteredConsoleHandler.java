package com.raindropcentral.rplatform.logging;

import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * FilteredConsoleHandler writes log messages to the console with level filtering and spam prevention.
 * 
 * <p>This handler implements two key features:
 * <ul>
 *   <li>Level filtering - Only messages at WARNING level or above are written to console by default</li>
 *   <li>Duplicate detection - Identical messages within a 5-second window are suppressed</li>
 * </ul>
 * 
 * <p>The handler writes to the original System.out stream (before any redirection) to avoid
 * recursion issues. It also performs periodic cleanup of old duplicate detection entries.
 * 
 * @see PluginLogger
 */
public class FilteredConsoleHandler extends Handler {
    
    private static final long DUPLICATE_WINDOW_MS = 5000; // 5 seconds
    private static final long CLEANUP_INTERVAL_MS = 60000; // 1 minute
    
    private final PrintStream originalOut;
    private final Map<String, Long> lastSeen;
    private long lastCleanupTime;
    private boolean closed;
    
    /**
     * Creates a new FilteredConsoleHandler that writes to the original System.out stream.
     * The default level is set to WARNING, meaning only WARNING and SEVERE messages are logged.
     * 
     * @param originalOut the original System.out stream (before any redirection)
     */
    public FilteredConsoleHandler(@NotNull PrintStream originalOut) {
        this.originalOut = originalOut;
        this.lastSeen = new ConcurrentHashMap<>();
        this.lastCleanupTime = System.currentTimeMillis();
        this.closed = false;
        
        // Set default level to WARNING (only WARN and SEVERE to console)
        setLevel(Level.WARNING);
    }
    
    /**
     * Publishes a log record to the console if it passes level filtering and duplicate detection.
     * 
     * @param record the log record to publish
     */
    @Override
    public synchronized void publish(LogRecord record) {
        // Check if handler is closed or record is null
        if (closed || record == null) {
            return;
        }
        
        // Check if record should be logged based on level
        if (!isLoggable(record)) {
            return;
        }
        
        try {
            // Get the formatted message
            String message = getFormatter().format(record);
            
            // Check for duplicate messages
            if (isDuplicate(message)) {
                return; // Skip duplicate
            }
            
            // Write to original console stream
            originalOut.print(message);
            
            // Perform periodic cleanup
            performPeriodicCleanup();
            
        } catch (Exception e) {
            // Report error but don't throw exception
            reportError("Error publishing log record to console", e, ErrorManager.WRITE_FAILURE);
        }
    }
    
    /**
     * Checks if a message is a duplicate within the time window.
     * If not a duplicate, records the message with the current timestamp.
     * 
     * @param message the message to check
     * @return true if the message is a duplicate, false otherwise
     */
    private boolean isDuplicate(@NotNull String message) {
        long now = System.currentTimeMillis();
        
        // Get the last time this message was seen
        Long lastTime = lastSeen.get(message);
        
        // Check if message was seen recently
        if (lastTime != null && (now - lastTime) < DUPLICATE_WINDOW_MS) {
            return true; // Duplicate within time window
        }
        
        // Record this message with current timestamp
        lastSeen.put(message, now);
        return false;
    }
    
    /**
     * Performs periodic cleanup of old entries from the duplicate detection map.
     * This prevents the map from growing unbounded over time.
     * Cleanup runs approximately once per minute.
     */
    private void performPeriodicCleanup() {
        long now = System.currentTimeMillis();
        
        // Check if it's time for cleanup
        if (now - lastCleanupTime < CLEANUP_INTERVAL_MS) {
            return;
        }
        
        lastCleanupTime = now;
        
        // Remove entries older than the duplicate window
        Iterator<Map.Entry<String, Long>> iterator = lastSeen.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (now - entry.getValue() > DUPLICATE_WINDOW_MS) {
                iterator.remove();
            }
        }
    }
    
    /**
     * Flushes any buffered output.
     * Since we write directly to PrintStream with auto-flush, this is a no-op.
     */
    @Override
    public synchronized void flush() {
        if (!closed && originalOut != null) {
            originalOut.flush();
        }
    }
    
    /**
     * Closes the handler and releases resources.
     * Clears the duplicate detection map.
     */
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        
        closed = true;
        
        // Clear the duplicate detection map
        lastSeen.clear();
        
        // Note: We don't close the originalOut stream as it's the system stream
    }
    
    /**
     * Gets the duplicate detection time window in milliseconds.
     * 
     * @return the time window in milliseconds (5000ms = 5 seconds)
     */
    public long getDuplicateWindowMs() {
        return DUPLICATE_WINDOW_MS;
    }
    
    /**
     * Gets the number of messages currently tracked in the duplicate detection map.
     * This is primarily for testing and diagnostics.
     * 
     * @return the number of tracked messages
     */
    public int getTrackedMessageCount() {
        return lastSeen.size();
    }
    
    /**
     * Checks if this handler has been closed.
     * 
     * @return true if closed, false otherwise
     */
    public boolean isClosed() {
        return closed;
    }
    
    /**
     * Manually triggers cleanup of old duplicate detection entries.
     * This is primarily for testing purposes.
     */
    void triggerCleanup() {
        lastCleanupTime = 0; // Force cleanup on next publish
    }
}
