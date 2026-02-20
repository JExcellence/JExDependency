package com.raindropcentral.rplatform.logging;

import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * RLogFormatter provides custom formatting for RPlatform log messages.
 * 
 * <p>Format pattern: {@code [HH:mm:ss LEVEL] [PluginName] Message}
 * 
 * <p>When logging from a non-main thread, the thread name is included:
 * {@code [HH:mm:ss LEVEL] [PluginName] [ThreadName] Message}
 * 
 * <p>Stack traces are formatted with proper indentation on separate lines.
 * 
 * <p>Features:
 * <ul>
 *   <li>Consistent timestamp format (HH:mm:ss)</li>
 *   <li>Plugin name identification</li>
 *   <li>Thread name for non-main threads</li>
 *   <li>Properly indented stack traces</li>
 *   <li>UTF-8 compatible output</li>
 * </ul>
 * 
 * @see PluginLogger
 * @see RotatingFileHandler
 * @see FilteredConsoleHandler
 */
public class RLogFormatter extends Formatter {
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    
    private static final String MAIN_THREAD_NAME = "Server thread";
    
    /**
     * Formats a log record according to the RPlatform logging format.
     * 
     * @param record the log record to format
     * @return the formatted log message
     */
    @Override
    public String format(@NotNull LogRecord record) {
        if (record == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // Format timestamp [HH:mm:ss]
        String timestamp = TIME_FORMATTER.format(Instant.ofEpochMilli(record.getMillis()));
        sb.append('[').append(timestamp).append(' ');
        
        // Format level [LEVEL]
        String level = formatLevel(record.getLevel().getName());
        sb.append(level).append(']');
        
        // Format plugin name [PluginName]
        String loggerName = record.getLoggerName();
        if (loggerName != null && !loggerName.isEmpty()) {
            sb.append(" [").append(loggerName).append(']');
        }
        
        // Add thread name if not main thread [ThreadName]
        String threadName = getThreadName(record);
        if (threadName != null && !isMainThread(threadName)) {
            sb.append(" [").append(threadName).append(']');
        }
        
        // Add message
        sb.append(' ');
        String message = formatMessage(record);
        sb.append(message);
        
        // Add newline if message doesn't end with one
        if (!message.endsWith("\n")) {
            sb.append('\n');
        }
        
        // Format stack trace if present
        Throwable throwable = record.getThrown();
        if (throwable != null) {
            sb.append(formatStackTrace(throwable));
        }
        
        return sb.toString();
    }
    
    /**
     * Formats the level name to a consistent width for alignment.
     * 
     * @param levelName the level name (e.g., "INFO", "WARNING", "SEVERE")
     * @return the formatted level name
     */
    @NotNull
    private String formatLevel(@NotNull String levelName) {
        // Map Java logging levels to shorter names
        switch (levelName) {
            case "SEVERE":
                return "SEVERE";
            case "WARNING":
                return "WARN";
            case "INFO":
                return "INFO";
            case "CONFIG":
                return "CONFIG";
            case "FINE":
                return "DEBUG";
            case "FINER":
                return "TRACE";
            case "FINEST":
                return "TRACE";
            default:
                return levelName;
        }
    }
    
    /**
     * Gets the thread name from the log record.
     * 
     * @param record the log record
     * @return the thread name, or null if not available
     */
    private String getThreadName(@NotNull LogRecord record) {
        // Try to get thread ID and resolve to name
        long threadId = record.getLongThreadID();
        
        // Find thread by ID
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.threadId() == threadId) {
                return thread.getName();
            }
        }
        
        // Fallback: return thread ID as string
        return "Thread-" + threadId;
    }
    
    /**
     * Checks if the given thread name is the main server thread.
     * 
     * @param threadName the thread name to check
     * @return true if this is the main thread, false otherwise
     */
    private boolean isMainThread(@NotNull String threadName) {
        return threadName.equals(MAIN_THREAD_NAME) || 
               threadName.equals("main") ||
               threadName.startsWith("Server");
    }
    
    /**
     * Formats a stack trace with proper indentation.
     * Each line of the stack trace is indented with spaces.
     * 
     * @param throwable the throwable to format
     * @return the formatted stack trace
     */
    @NotNull
    private String formatStackTrace(@NotNull Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        // Write the stack trace to the string writer
        throwable.printStackTrace(pw);
        pw.flush();
        
        // Get the stack trace as a string
        String stackTrace = sw.toString();
        
        // Split into lines and indent each line
        StringBuilder sb = new StringBuilder();
        String[] lines = stackTrace.split("\n");
        
        for (String line : lines) {
            // Add indentation for stack trace lines
            if (line.startsWith("\tat ") || line.startsWith("Caused by:") || line.startsWith("\t...")) {
                sb.append("    ").append(line.trim()).append('\n');
            } else {
                // Exception message line
                sb.append(line).append('\n');
            }
        }
        
        return sb.toString();
    }
}
