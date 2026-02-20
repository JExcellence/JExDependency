package com.raindropcentral.rplatform.logging;

import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.logging.Level;

/**
 * SafeLoggingPrintStream is a custom PrintStream that redirects output to the logging system
 * while providing recursion protection and fallback to original streams on errors.
 * 
 * <p>This class is used to safely redirect System.out and System.err to the centralized
 * logging system without causing infinite recursion or StackOverflowError.
 * 
 * <p>Features:
 * <ul>
 *   <li>Redirects print operations to PluginLogger</li>
 *   <li>Recursion protection using thread-local counters</li>
 *   <li>Automatic fallback to original stream on errors</li>
 *   <li>Handles both String and Object println methods</li>
 * </ul>
 */
class SafeLoggingPrintStream extends PrintStream {
    
    private static final int MAX_RECURSION_DEPTH = 2;
    
    private final PluginLogger logger;
    private final Level level;
    private final PrintStream originalStream;
    private final ThreadLocal<Integer> recursionDepth;
    
    /**
     * Creates a new SafeLoggingPrintStream that redirects to the specified logger.
     * 
     * @param logger the PluginLogger to redirect output to
     * @param level the log level to use (INFO for stdout, SEVERE for stderr)
     * @param originalStream the original stream to fall back to on errors
     */
    SafeLoggingPrintStream(@NotNull PluginLogger logger, @NotNull Level level, @NotNull PrintStream originalStream) {
        // Call super with a dummy OutputStream (we override all methods)
        super(new OutputStream() {
            @Override
            public void write(int b) {
                // Not used - all operations are overridden
            }
        });
        
        this.logger = logger;
        this.level = level;
        this.originalStream = originalStream;
        this.recursionDepth = ThreadLocal.withInitial(() -> 0);
    }
    
    /**
     * Checks if we can safely log or if we should fall back to the original stream.
     * 
     * @return true if logging should proceed, false if fallback is needed
     */
    private boolean canLog() {
        int depth = recursionDepth.get();
        
        // If we've exceeded recursion limit, use fallback
        if (depth >= MAX_RECURSION_DEPTH) {
            return false;
        }
        
        // If logger is in emergency mode, use fallback
        if (logger.isEmergencyMode()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Enters a logging operation by incrementing the recursion counter.
     */
    private void enterLogging() {
        int depth = recursionDepth.get();
        recursionDepth.set(depth + 1);
    }
    
    /**
     * Exits a logging operation by decrementing the recursion counter.
     */
    private void exitLogging() {
        int depth = recursionDepth.get();
        if (depth > 0) {
            recursionDepth.set(depth - 1);
        }
    }
    
    /**
     * Logs a message through the logger or falls back to the original stream.
     * 
     * @param message the message to log
     */
    private void logMessage(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        
        if (!canLog()) {
            // Fallback to original stream
            originalStream.println(message);
            return;
        }
        
        enterLogging();
        try {
            // Route to appropriate log level
            if (level == Level.INFO) {
                logger.info(message);
            } else if (level == Level.SEVERE) {
                logger.severe(message);
            } else {
                logger.warning(message);
            }
        } catch (Exception e) {
            // On any error, fall back to original stream
            originalStream.println(message);
        } finally {
            exitLogging();
        }
    }
    
    // ========== Override println methods ==========
    
    @Override
    public void println(String x) {
        logMessage(x);
    }
    
    @Override
    public void println(Object x) {
        if (x == null) {
            logMessage("null");
        } else {
            logMessage(x.toString());
        }
    }
    
    @Override
    public void println() {
        // Empty line - just log empty string
        logMessage("");
    }
    
    @Override
    public void println(boolean x) {
        logMessage(String.valueOf(x));
    }
    
    @Override
    public void println(char x) {
        logMessage(String.valueOf(x));
    }
    
    @Override
    public void println(int x) {
        logMessage(String.valueOf(x));
    }
    
    @Override
    public void println(long x) {
        logMessage(String.valueOf(x));
    }
    
    @Override
    public void println(float x) {
        logMessage(String.valueOf(x));
    }
    
    @Override
    public void println(double x) {
        logMessage(String.valueOf(x));
    }
    
    @Override
    public void println(char[] x) {
        logMessage(String.valueOf(x));
    }
    
    // ========== Override print methods ==========
    
    @Override
    public void print(String s) {
        // For print (without newline), we still log it
        // This ensures partial lines are captured
        if (s != null && !s.isEmpty()) {
            logMessage(s);
        }
    }
    
    @Override
    public void print(Object obj) {
        if (obj != null) {
            logMessage(obj.toString());
        }
    }
    
    @Override
    public void print(boolean b) {
        logMessage(String.valueOf(b));
    }
    
    @Override
    public void print(char c) {
        logMessage(String.valueOf(c));
    }
    
    @Override
    public void print(int i) {
        logMessage(String.valueOf(i));
    }
    
    @Override
    public void print(long l) {
        logMessage(String.valueOf(l));
    }
    
    @Override
    public void print(float f) {
        logMessage(String.valueOf(f));
    }
    
    @Override
    public void print(double d) {
        logMessage(String.valueOf(d));
    }
    
    @Override
    public void print(char[] s) {
        logMessage(String.valueOf(s));
    }
    
    // ========== Override printf/format methods ==========
    
    @Override
    public PrintStream printf(String format, Object... args) {
        try {
            String message = String.format(format, args);
            logMessage(message);
        } catch (Exception e) {
            // If formatting fails, fall back to original stream
            originalStream.printf(format, args);
        }
        return this;
    }
    
    @Override
    public PrintStream printf(Locale l, String format, Object... args) {
        try {
            String message = String.format(l, format, args);
            logMessage(message);
        } catch (Exception e) {
            // If formatting fails, fall back to original stream
            originalStream.printf(l, format, args);
        }
        return this;
    }
    
    @Override
    public PrintStream format(String format, Object... args) {
        return printf(format, args);
    }
    
    @Override
    public PrintStream format(Locale l, String format, Object... args) {
        return printf(l, format, args);
    }
    
    // ========== Override flush and close ==========
    
    @Override
    public void flush() {
        // Flush the logger
        try {
            logger.flush();
        } catch (Exception e) {
            // Ignore flush errors
        }
        
        // Also flush original stream
        originalStream.flush();
    }
    
    @Override
    public void close() {
        // Don't close the logger - it's managed by CentralLogger
        // Just flush pending output
        flush();
    }
}
